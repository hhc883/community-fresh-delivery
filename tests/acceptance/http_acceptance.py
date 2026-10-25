"""External HTTP acceptance. Invoke through scripts/run-acceptance.py only.

Writes only to a newly initialized cfd_acceptance_* database in the child server.
No direct SQL assertions and no application imports; expected money is literal.
"""
import concurrent.futures
from decimal import Decimal
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.request
import uuid


class Acceptance:
    def __init__(self, base_url):
        self.url = base_url
        self.results = []
        self.admin = None
        self.users = []
        self.run_id = uuid.uuid4().hex[:10]

    def request(self, method, path, body=None, token=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        req = urllib.request.Request(self.url + path, method=method, headers=headers,
                                     data=None if body is None else json.dumps(body).encode())
        try:
            with urllib.request.urlopen(req, timeout=20) as response:
                return response.status, json.loads(response.read())
        except urllib.error.HTTPError as error:
            raw = error.read()
            try:
                body = json.loads(raw)
            except ValueError:
                body = {"error": raw.decode(errors="replace")[:300]}
            return error.code, body

    def ok(self, method, path, body=None, token=None):
        status, response = self.request(method, path, body, token)
        expected = 200 if path.startswith("/api/") else 1
        assert status == 200 and response.get("code") == expected, (method, path, status, response)
        return response.get("data")

    def rejected(self, method, path, body=None, token=None):
        status, response = self.request(method, path, body, token)
        expected = 200 if path.startswith("/api/") else 1
        assert status in (400, 401, 403) or (status == 200 and response.get("code") != expected), (path, status, response)

    def login(self, index):
        phone = "139" + str((int(time.time() * 1000) + index) % 100000000).zfill(8)
        data = self.ok("POST", "/api/auth/login", {"phone": phone, "code": os.environ.get("AUTH_DEV_CODE", "123456")})
        return data["token"]

    def order(self, token, dish=1, quantity=1, **extra):
        body = {"pickupPointId": "1", "pickupPersonName": "验收虚构用户", "pickupPersonPhone": "13900000001",
                "clientRequestId": self.run_id + "-" + uuid.uuid4().hex, "amount": "0.01",
                "items": [{"dishId": str(dish), "quantity": quantity, "price": "0.01", "name": "伪造商品名"}]}
        body.update(extra)
        return body

    def detail(self, order_id, token):
        return self.ok("GET", "/api/orders/" + str(order_id), token=token)

    def stock(self, quantity):
        dish = self.ok("GET", "/admin/dish/1", token=self.admin)
        dish["stock"] = quantity
        self.ok("PUT", "/admin/dish", dish, self.admin)

    def case(self, identifier, label, action):
        start = time.monotonic()
        try:
            action()
            result = {"id": identifier, "name": label, "status": "PASS"}
        except Exception as error:
            result = {"id": identifier, "name": label, "status": "FAIL", "detail": str(error)}
        result["seconds"] = round(time.monotonic() - start, 3)
        self.results.append(result)
        print(identifier, result["status"], label, flush=True)

    def setup(self):
        data = self.ok("POST", "/admin/employee/login", {"username": os.environ.get("ACCEPTANCE_ADMIN_USER", "admin"),
                      "password": os.environ["ACCEPTANCE_ADMIN_PASSWORD"]})
        self.admin = data["token"]
        self.users = [self.login(index) for index in range(8)]

    def addresses(self):
        first, second = self.users[:2]
        self.ok("POST", "/user/addressBook", {"consignee": "验收用户", "phone": "13900000001", "sex": "1",
                "provinceName": "测试省", "cityName": "测试市", "districtName": "测试区", "detail": "虚构测试路1号", "isDefault": 0}, first)
        records = self.ok("GET", "/user/addressBook/list", token=first)
        self.address = records[-1]["id"]
        self.ok("PUT", "/user/addressBook/default", {"id": self.address}, first)
        default = self.ok("GET", "/user/addressBook/default", token=first)
        assert str(default["id"]) == str(self.address)
        self.rejected("GET", "/user/addressBook/" + str(self.address), token=second)
        self.rejected("DELETE", "/user/addressBook?id=" + str(self.address), token=second)

    def product(self):
        self.stock(100)
        self.ok("POST", "/admin/dish/status/0?id=1", token=self.admin)
        self.rejected("POST", "/api/orders", self.order(self.users[0]), self.users[0])
        self.ok("POST", "/admin/dish/status/1?id=1", token=self.admin)

    def price(self):
        token = self.users[0]
        created = self.ok("POST", "/api/orders", self.order(token, quantity=2), token)
        assert Decimal(str(created["orderAmount"])) == Decimal("25.00"), created
        for quantity in (-1, 0, 999999):
            self.rejected("POST", "/api/orders", self.order(token, quantity=quantity), token)
        self.ok("POST", "/api/orders/" + str(created["id"]) + "/cancel", token=token)

    def duplicate_and_stock(self):
        token = self.users[0]
        body = self.order(token)
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
            repeats = list(executor.map(lambda _: self.ok("POST", "/api/orders", body, token), range(8)))
        first = repeats[0]
        assert len({str(item["id"]) for item in repeats}) == 1, repeats
        self.ok("POST", "/api/orders/" + str(first["id"]) + "/cancel", token=token)
        self.stock(1)

        def buy(user):
            return user, self.request("POST", "/api/orders", self.order(user), user)

        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
            responses = list(executor.map(buy, self.users))
        winners = [(user, response[1]["data"]) for user, response in responses
                   if response[0] == 200 and response[1].get("code") == 200]
        assert len(winners) == 1, [(r[1][0], r[1][1].get("code")) for r in responses]
        dish = self.ok("GET", "/admin/dish/1", token=self.admin)
        assert dish["stock"] == 0, dish
        user, order = winners[0]
        self.ok("POST", "/api/orders/" + str(order["id"]) + "/cancel", token=user)
        dish = self.ok("GET", "/admin/dish/1", token=self.admin)
        assert dish["stock"] == 1, dish
        self.stock(100)

    def ownership(self):
        empty = self.ok("GET", "/api/orders?pageNum=1&pageSize=10", token=self.login(99))
        assert empty["total"] == 0 and empty["records"] == [], empty
        order = self.ok("POST", "/api/orders", self.order(self.users[0]), self.users[0])
        self.rejected("GET", "/api/orders/" + str(order["id"]), token=self.users[1])
        self.rejected("POST", "/api/pay/" + str(order["id"]) + "/mock-pay", token=self.users[1])
        self.rejected("GET", "/api/orders/9223372036854775806", token=self.users[0])
        self.rejected("POST", "/api/pay/9223372036854775806/mock-pay", token=self.users[0])
        self.ok("POST", "/api/orders/" + str(order["id"]) + "/cancel", token=self.users[0])

    def payment_and_pickup(self):
        token = self.users[0]
        order = self.ok("POST", "/api/orders", self.order(token), token)
        path = "/api/pay/" + str(order["id"]) + "/mock-pay"
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
            codes = list(executor.map(lambda _: self.ok("POST", path, token=token), range(8)))
        code = codes[0]
        assert all(value == code for value in codes), codes
        self.rejected("POST", "/admin/order/verify", {"pickupCode": code, "pickupPointId": "2"}, self.admin)
        self.ok("POST", "/admin/order/verify", {"pickupCode": code, "pickupPointId": "1"}, self.admin)
        self.rejected("POST", "/admin/order/verify", {"pickupCode": code, "pickupPointId": "1"}, self.admin)
        assert self.detail(order["id"], token)["status"] == 5
        self.restart_order = order["id"]
        self.rejected("POST", "/api/orders/" + str(order["id"]) + "/cancel", token=token)
        self.ok("POST", path, token=token)
        assert self.detail(order["id"], token)["status"] == 5
        cancelled = self.ok("POST", "/api/orders", self.order(token), token)
        self.ok("POST", "/api/orders/" + str(cancelled["id"]) + "/cancel", token=token)
        self.rejected("POST", "/api/pay/" + str(cancelled["id"]) + "/mock-pay", token=token)

    def delivery(self):
        token = self.users[0]
        body = self.order(token, addressBookId=self.address)
        for field in ("pickupPointId", "pickupPersonName", "pickupPersonPhone"):
            body.pop(field)
        order = self.ok("POST", "/api/orders", body, token)
        identifier = str(order["id"])
        self.rejected("PUT", "/admin/order/complete/" + identifier, token=self.admin)
        self.ok("POST", "/api/pay/" + identifier + "/mock-pay", token=token)
        self.ok("PUT", "/admin/order/confirm", {"id": identifier}, self.admin)
        self.ok("PUT", "/admin/order/delivery/" + identifier, token=self.admin)
        self.ok("PUT", "/admin/order/complete/" + identifier, token=self.admin)
        assert self.detail(identifier, token)["status"] == 5

    def roles(self):
        for token in (None, self.users[0]):
            status, _ = self.request("GET", "/admin/order/conditionSearch?page=1&pageSize=10", token=token)
            assert status == 401, status
        status, _ = self.request("GET", "/api/orders", token=self.admin)
        assert status == 401, status

    def verify_restart(self):
        assert self.detail(self.restart_order, self.users[0])["status"] == 5

    def groups(self):
        first, second = self.users[:2]
        recipient = {"pickupPointId": "1", "pickupPersonName": "虚构拼团用户", "pickupPersonPhone": "13900000001"}
        group = self.ok("POST", "/user/groupon/open", dict(recipient, dishId="1", number=1), first)
        group_id = str(group["id"])
        self.rejected("POST", "/user/groupon/join", dict(recipient, grouponId=group_id, number=1), first)
        self.ok("POST", "/user/groupon/join", dict(recipient, grouponId=group_id, number=1), second)
        detail = self.ok("GET", "/user/groupon/detail/" + group_id, token=first)
        assert detail["status"] == 2 and detail["currentSize"] == 2, detail
        self.ok("PUT", "/admin/groupon/closeExpired", token=self.admin)

    def expired_fixture(self):
        self.ok("PUT", "/admin/groupon/closeExpired", token=self.admin)
        order = self.ok("GET", "/admin/order/details/2", token=self.admin)
        assert order["status"] == 6 and order["payStatus"] == 2, order
        dish = self.ok("GET", "/admin/dish/1", token=self.admin)
        assert dish["stock"] == 100, dish
        self.ok("PUT", "/admin/groupon/closeExpired", token=self.admin)
        dish = self.ok("GET", "/admin/dish/1", token=self.admin)
        assert dish["stock"] == 100, dish

    def reports(self):
        # Independent worked fixture: three completed units at 12.50 on 2000-01-01.
        turnover = self.ok("GET", "/admin/report/turnoverStatistics?begin=2000-01-01&end=2000-01-02", token=self.admin)
        assert turnover["dateList"] == "2000-01-01,2000-01-02", turnover
        assert [Decimal(x) for x in turnover["turnoverList"].split(",")] == [Decimal("37.50"), Decimal("0")], turnover
        top = self.ok("GET", "/admin/report/top10?begin=2000-01-01&end=2000-01-01", token=self.admin)
        assert top["numberList"] == "3", top
        groups = self.ok("GET", "/admin/report/grouponStatistics?begin=2001-01-01&end=2001-01-01", token=self.admin)
        assert groups["expired"] == 1 and groups["memberCount"] == 1 and groups["grouponOrderCount"] == 1, groups
        assert Decimal(str(groups["grouponTurnover"])) == Decimal("0"), groups
        self.rejected("GET", "/admin/report/ordersStatistics?begin=2000-01-02&end=2000-01-01", token=self.admin)

    def run(self):
        self.case("SETUP", "管理员与八名隔离用户登录", self.setup)
        if not self.users:
            return self.results
        for identifier, label, action in [
            ("A05", "到期团订单模拟退款与库存仅回补一次", self.expired_fixture),
            ("A02", "地址默认与用户隔离", self.addresses),
            ("A03", "库存管理与商品上下架", self.product),
            ("A06", "服务端12.50元计价与非法数量拒绝", self.price),
            ("A07", "重复下单与八用户竞争最后一件库存", self.duplicate_and_stock),
            ("A08", "真实空列表与订单归属", self.ownership),
            ("A09/A10", "模拟支付幂等与自提核销终态", self.payment_and_pickup),
            ("A10", "上门配送合法状态推进", self.delivery),
            ("A11", "用户与管理员JWT不可互换", self.roles),
            ("A04/A05", "两用户成团与重复参团拒绝", self.groups),
            ("A12", "2000年固定样例报表37.50元与3件销量", self.reports)]:
            self.case(identifier, label, action)
        return self.results


if __name__ == "__main__":
    raise SystemExit("Use scripts/run-acceptance.py so the test server is launched with a verified isolated database.")
