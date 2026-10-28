import test from "node:test";
import assert from "node:assert/strict";
import { checkout, clearPendingCheckout } from "../services/checkout.mjs";

test("retrying an interrupted checkout replays one intent without trusting client prices", async () => {
  const storage = new Map();
  const requests = [];
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
    request: (options) => {
      requests.push(options.data);
      if (requests.length === 1) options.fail();
      else
        options.success({
          statusCode: 200,
          data: {
            code: 200,
            data: { id: "9007199254740993", orderAmount: 12.5 },
          },
        });
    },
  };
  const intent = {
    items: [{ id: "9007199254740991", quantity: 2, price: 0.01 }],
    deliveryType: "delivery",
    addressBookId: "12",
    amount: 0.02,
  };
  await assert.rejects(checkout(intent), (error) => error.code === -1);
  const result = await checkout(intent);
  assert.equal(result.id, "9007199254740993");
  assert.equal(requests[0].clientRequestId, requests[1].clientRequestId);
  assert.match(
    requests[0].clientRequestId,
    /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
  );
  assert.deepEqual(requests[0].items, [
    { dishId: "9007199254740991", number: 2 },
  ]);
  assert.equal(requests[0].deliveryType, 1);
  assert.equal(requests[0].amount, undefined);
});

test("changing cart quantities produces a fresh clientRequestId UUID", async () => {
  const storage = new Map();
  const requests = [];
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
    request: (options) => {
      requests.push(options.data);
      options.fail();
    },
  };
  const intent1 = {
    items: [{ id: "9007199254740991", quantity: 2 }],
    deliveryType: "delivery",
    addressBookId: "12",
  };
  const intent2 = {
    items: [{ id: "9007199254740991", quantity: 3 }],
    deliveryType: "delivery",
    addressBookId: "12",
  };
  await assert.rejects(checkout(intent1));
  await assert.rejects(checkout(intent2));
  assert.equal(requests.length, 2);
  assert.notEqual(requests[0].clientRequestId, requests[1].clientRequestId);
});

test("changing delivery or address produces a fresh clientRequestId UUID", async () => {
  const storage = new Map();
  const requests = [];
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
    request: (options) => {
      requests.push(options.data);
      options.fail();
    },
  };
  const deliveryIntent = {
    items: [{ id: "100", quantity: 1 }],
    deliveryType: "delivery",
    addressBookId: "12",
  };
  const pickupIntent = {
    items: [{ id: "100", quantity: 1 }],
    deliveryType: "pickup",
    pickupPointId: "201",
    pickupPersonName: "李四",
    pickupPersonPhone: "13900000000",
  };
  await assert.rejects(checkout(deliveryIntent));
  await assert.rejects(checkout(pickupIntent));
  assert.equal(requests.length, 2);
  assert.notEqual(requests[0].clientRequestId, requests[1].clientRequestId);
  assert.equal(requests[0].deliveryType, 1);
  assert.equal(requests[1].deliveryType, 2);
});

test("changing remark produces a fresh clientRequestId UUID", async () => {
  const storage = new Map();
  const requests = [];
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
    request: (options) => {
      requests.push(options.data);
      options.fail();
    },
  };
  const intent1 = {
    items: [{ id: "100", quantity: 1 }],
    deliveryType: "delivery",
    addressBookId: "12",
    remark: "放门口",
  };
  const intent2 = {
    items: [{ id: "100", quantity: 1 }],
    deliveryType: "delivery",
    addressBookId: "12",
    remark: "放门卫",
  };
  await assert.rejects(checkout(intent1));
  await assert.rejects(checkout(intent2));
  assert.equal(requests.length, 2);
  assert.notEqual(requests[0].clientRequestId, requests[1].clientRequestId);
});

test("changing groupon target or mode produces a fresh clientRequestId UUID", async () => {
  const storage = new Map();
  const requests = [];
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
    request: (options) => {
      requests.push(options.data);
      options.fail();
    },
  };
  const openIntent = {
    mode: "open",
    dishId: "501",
    quantity: 1,
    deliveryType: "pickup",
    pickupPointId: "201",
    pickupPersonName: "王五",
    pickupPersonPhone: "13700000000",
  };
  const joinIntent = {
    mode: "join",
    grouponId: "9007199254740995",
    quantity: 1,
    deliveryType: "pickup",
    pickupPointId: "201",
    pickupPersonName: "王五",
    pickupPersonPhone: "13700000000",
  };
  await assert.rejects(checkout(openIntent));
  await assert.rejects(checkout(joinIntent));
  assert.equal(requests.length, 2);
  assert.notEqual(requests[0].clientRequestId, requests[1].clientRequestId);
});

test("clearPendingCheckout clears stored intent for user and guest", () => {
  const storage = new Map();
  storage.set("pending-checkout:user_999", { fingerprint: "fp1", clientRequestId: "uuid-1" });
  storage.set("pending-checkout:guest", { fingerprint: "fp2", clientRequestId: "uuid-2" });
  globalThis.uni = {
    removeStorageSync: (key) => storage.delete(key),
  };
  clearPendingCheckout("user_999");
  assert.equal(storage.has("pending-checkout:user_999"), false);
  assert.equal(storage.has("pending-checkout:guest"), false);
});
