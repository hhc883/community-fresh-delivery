# 接口契约 v1（管理后台面向真实后端）

状态：**已冻结（v1，2026-10-24）**。维护人：本契约由 GLM 在 M0 阶段代拟并冻结，后续变更按本文末尾的变更规则执行。
适用端：`front-admin/`（运营管理后台）。`front/`、`front-merchant/`、`front-partner/` 的契约随各自任务另行补充。

## 0 基线决定

1. **管理后台直连 `backEnd/`（Spring Boot 2.7.3，`com/sky`，端口 8080）**，复用既有 `/admin/**` 接口；
   不新增 `/api/admin/**` 适配层。旧 `backEnd_sb3_backup/`（com/fresh，3001）仅作参考，不是联调对象。
2. 仅新增两个薄接口（均挂在既有 `/admin/**` 命名空间下，复用既有 service）：
   - `GET /admin/groupon/page` —— 拼团活动分页列表；
   - `GET /admin/report/grouponStatistics` —— 拼团统计。
3. 管理端鉴权统一用员工 JWT（`POST /admin/employee/login`），`Authorization: Bearer <token>`。
   禁止用小程序用户 token 访问管理接口。

## 1 通用约定

| 项 | 约定 |
| --- | --- |
| 成功码 | `/admin/**` 信封 `Result`：`code=1` 成功；失败 `code=0` 且 `msg/message` 携带原因。前端只认 `code===1` |
| HTTP 状态 | 业务失败仍为 HTTP 200（GlobalExceptionHandler 统一返回 Result.error）；唯一例外：**管理端 JWT 校验失败回 HTTP 401**，body 为 `{"code":401,"message":"请先登录"}` |
| 金额 | 后端 `decimal(10,2)`，JSON 中为数字；前端仅展示与录入，不参与服务端计价 |
| 时间 | 数据库 `Asia/Shanghai`；报表区间参数格式 `yyyy-MM-dd`（beginDate/endDate），返回的曲线数据 dateList 为 `MM-dd` |
| 大整数 ID | 订单 id 为雪花/长整型，前端透传字符串，禁止 `Number()` 化后参与运算或再回传 |
| 分页 | 请求 `page/pageSize`（≥1，pageSize ≤ 100），响应 `PageResult{total, records}` |
| 登录态失效 | HTTP 401 → 前端清 token、跳登录页，不重试 |

## 2 管理后台接口清单（全部真实存在于 backEnd/，标注新增）

### 认证
| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/employee/login` | POST | body `{username,password}`；成功 data `{id,userName,name,token}` |
| `/admin/employee/logout` | POST | 退出（前端同时清本地 token） |

### 大盘（WorkSpace）
| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/workspace/businessData` | GET | 营业额、有效订单、完成率、客单价、新增用户 |
| `/admin/workspace/overviewOrders` | GET | 各状态订单数（待接单/已接单/派送中/待审核取消） |
| `/admin/workspace/overviewDishes` | GET | 起售/停售商品数 |

### 商品与分类
| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/dish/page` | GET | `name/categoryId/status/page/pageSize`；含 stock、isGroupon 等字段 |
| `/admin/dish` | POST/PUT | 新增/编辑（DishDTO：含 stock、isGroupon、grouponPrice、grouponSize、grouponValidHours、unit、origin、isFresh） |
| `/admin/dish/{id}` | GET | 详情（含口味） |
| `/admin/dish/status/{status}?id=` | POST | 起售(1)/停售(0) |
| `/admin/dish?ids=` | DELETE | 批量删除 |
| `/admin/category/list?type=` | GET | 分类列表 |
| `/admin/category` | POST/PUT | 新增/编辑分类 |
| `/admin/category/status/{status}?id=` | POST | 分类启用/禁用 |
| `/admin/category?id=` | DELETE | 删除分类 |

### 订单履约
| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/order/conditionSearch` | GET | `page/pageSize/number/phone/status/beginTime/endTime`（时间 `yyyy-MM-dd HH:mm:ss`） |
| `/admin/order/details/{id}` | GET | 订单详情（含明细、自提点信息、pickupCode） |
| `/admin/order/confirm` | PUT | 接单 `{id}` |
| `/admin/order/rejection` | PUT | 拒单 `{id,rejectionReason}` |
| `/admin/order/cancel` | PUT | 取消 `{id,cancelReason}` |
| `/admin/order/delivery/{id}` | PUT | 开始派送 |
| `/admin/order/complete/{id}` | PUT | 完成 |
| `/admin/order/verify` | POST | 提货码核销（OrderVerifyDTO） |
| `/admin/order/statistics` | GET | 待接单/已接单/派送中计数 |

### 拼团管理（**新增**）
| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/groupon/page`（新增） | GET | `page/pageSize/status(1拼团中 2已成团 3已失败 4已过期)/keyword(商品名模糊)`；data `PageResult{total, records:GrouponVO[]}`，GrouponVO 含 dishName、currentSize、requiredSize、lackSize、expireTime、status、pickupPointName |
| `/admin/groupon/complete/{id}` | PUT | 手动校验并成团（幂等：已成团时返回 false/成功） |
| `/admin/groupon/closeExpired` | PUT | 关闭全部超时未成团活动，data 为关闭数量 |

### 报表统计
| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/report/turnoverStatistics?begin=&end=` | GET | 营业额趋势（dateList/turnoverList，当天无成交为 0） |
| `/admin/report/ordersStatistics?begin=&end=` | GET | 订单数/有效订单趋势 |
| `/admin/report/userStatistics?begin=&end=` | GET | 新增用户趋势 |
| `/admin/report/top10?begin=&end=` | GET | 销量 Top10 商品（nameList/numberList） |
| `/admin/report/grouponStatistics`（新增） | GET | `begin/end` 可空（默认当日）；data：{ongoing,success,failed,expired,memberCount,grouponOrderCount,grouponTurnover} |

## 3 错误与边界行为（对齐验收用例）

- 任何写操作失败：HTTP 200 + `code=0` + 中文 message；前端原样展示，不本地造成功。
- `GET /admin/order/details/{id}` 无此单：`code=0`（不存在订单不返回演示数据）。
- 重复核销同一订单：服务端按 `verify_status` 判定，第二次核销返回失败，不产生第二次履约。
- 修改客户端价格不影响成交价：服务端计价（契约事实，前端金额仅展示）。

## 4 变更规则

契约变更先改本文档、注明影响端与兼容办法，再提交实现；不允许前端先猜字段。
