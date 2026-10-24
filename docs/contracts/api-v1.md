# API v1：首期对接契约

日期：2026-10-24。初始基线：`929851e`；已整合 Gemini/GLM 的远端基线 `0e6a49c`。适用范围：用户小程序、平台管理后台、Codex 后端和 DeepSeek 验收。

本契约区分现有路由与待实现能力。静态路由全表见 [interface-inventory.md](interface-inventory.md)。未列为已验证的接口不能写入验收通过项。

## 通用约定

- 后端为 Spring Boot **2.7.3**，包名 `com.sky`，三个 Maven 模块；默认端口 **8080**。
- 新用户端复用 `/api/**`；地址和拼团复用 `/user/**`；管理端优先适配已有 `/admin/**`。保留同一份业务 service，不重新维护第二套交易逻辑。
- `/api/**` 成功信封：`{code:200,data:...,msg:"...",message:"..."}`，一般业务失败 `code:400`。`/user/**`、`/admin/**` 成功为 `code:1`，一般业务失败为 `code:0`。由 `ResultResponseAdvice` 适配。
- 未登录/错误角色使用 HTTP 401；业务失败保留现有 HTTP 200 + 失败信封。语法或参数绑定失败使用 HTTP 400。前端请求层同时判断 HTTP 与业务 code。
- Bearer token 为首选，兼容旧 `token`、`authentication` 和配置的 token header。用户与管理员使用不同 claim（`userId` / `empId`），不可互换。
- 所有 Long 标识按字符串传输；数量为正整数。金额以人民币元表示，由服务端 `BigDecimal` 计算并保留两位小数。客户端价格、总价、名称和库存均不可信。
- 业务时间统一 `Asia/Shanghai`，日期 `yyyy-MM-dd`，日期时间 `yyyy-MM-dd HH:mm:ss`，Unix 时间戳若使用则单位为毫秒。
- 当前分页响应为 `{total,records}`。`/api/orders` 请求为 `pageNum`、`pageSize`；旧接口保留 `page`、`pageSize`。目标范围 `page>=1`、`1<=pageSize<=100`，待各入口逐步校验。

## 最小角色矩阵

| 操作 | 匿名 | 用户 JWT | 管理员 JWT |
| --- | --- | --- | --- |
| 商品、分类、自提点公开列表 | 允许 | 允许 | 公开访问 |
| 验证码/密码登录 | 允许 | 允许 | 公开访问 |
| 设置用户密码、维护地址 | 拒绝 | 仅本人 | 不得代替用户 token |
| 提交、查看、支付、取消用户订单 | 拒绝 | 仅本人 | 使用管理端接口 |
| 开团、参团、我的团购 | 拒绝 | 仅本人 | 使用管理端接口 |
| 商品维护、履约、核销、报表 | 拒绝 | 拒绝 | 允许 |
| 独立商家与自提点运营 | 不启用 | 不启用 | 后续扩展角色 |

用户身份必须来自验签后的 claim；请求体的 userId 不作为归属依据。不存在和他人订单使用相同的业务错误，避免枚举订单。

## 核心路由与当前状态

| 能力 | 方法和路径 | 请求/响应要点 | 状态与负责人 |
| --- | --- | --- | --- |
| 手机号登录 | `POST /api/auth/login` | `{phone,code}` → `{token,user,...}` | 路由已有；验证码验证需补强，Codex |
| 密码登录 | `POST /api/auth/login-password` | `{phone,password}` → token | 已有；旧密码兼容待整理，Codex |
| 设置密码 | `POST /api/auth/set-password` | `{password}`；必须用户鉴权 | 本批修复放行规则，Codex |
| 管理员登录 | `POST /admin/employee/login` | `{username,password}` → `{id,token,...}` | 路由已有，GLM 适配 |
| 商品 | `GET /api/dishes`、`GET /api/dishes/{id}` | 分类、keyword、categoryId；商品 VO | 已有；假商品兜底待清理，Codex |
| 自提点 | `GET /api/stores`、`GET /user/pickupPoint/list` | 使用数据库自提点 ID | 已有，Gemini 对接 |
| 地址列表/新增/编辑 | `GET /user/addressBook/list`、`POST/PUT /user/addressBook` | 地址字段、字符串 id | 已有；归属校验本批修复，Codex；页面 Gemini |
| 地址详情/删除/默认 | `GET /user/addressBook/{id}`、`DELETE /user/addressBook?id=...`、`PUT/GET /user/addressBook/default` | 设置默认传 `{id}` | 已有；归属校验本批修复 |
| 下单 | `POST /api/orders` 或 `POST /user/order/submit` | DTO 见下；返回 OrderSubmitVO | 两入口统一服务待实现，Codex |
| 用户订单列表 | `GET /api/orders?pageNum=1&pageSize=10&status=...` | `{total,records:[OrderVO]}` | 本批移除示例订单 |
| 用户订单详情 | `GET /api/orders/{id}`、`GET /user/order/orderDetail/{id}` | OrderVO，须本人订单 | 本批修复归属与缺失行为 |
| 用户取消 | `POST /api/orders/{id}/cancel`、`PUT /user/order/cancel/{id}` | 未付款/待接单可取消；完成、配送中拒绝 | 本批统一真实失败和归属；库存回补后续 |
| 模拟支付 | `POST /api/pay/{id}/mock-pay`、`POST /user/order/{id}/mock-pay` | data 为提货码；配送单不应依赖提货码 | 本批禁止不存在/他人/已取消订单；并发状态后续 |
| 旧模拟支付 | `PUT /user/order/payment` | `{orderNumber,payMethod}` | 必须校验归属，与模拟支付共用服务 |
| 拼团商品/可加入团 | `GET /user/groupon/dish/list`、`GET /user/groupon/joinable/{dishId}` | Dish/GrouponVO 列表 | 已有；仅商品列表允许匿名 |
| 开团/参团 | `POST /user/groupon/open`、`POST /user/groupon/join` | GrouponJoinDTO；返回 Groupon | 已有；事务、库存和到期回收待验证 |
| 团详情/我的团 | `GET /user/groupon/detail/{id}`、`GET /user/groupon/mine` | GrouponVO | 已有；成员敏感信息需核验 |
| 后台订单/详情 | `GET /admin/order/conditionSearch`、`GET /admin/order/details/{id}` | OrdersPageQueryDTO/OrderVO | 已有，管理员鉴权，GLM 对接 |
| 接单/配送/完成 | `PUT /admin/order/confirm`、`PUT /admin/order/delivery/{id}`、`PUT /admin/order/complete/{id}` | confirm body `{id}` | 已有；完整条件更新后续 |
| 核销 | `POST /admin/order/verify` | `{pickupCode,pickupPointId?}` → OrderVO | 已有原子更新；支付/终态条件需补强 |
| 商品维护 | `/admin/dish`、`/admin/category` | 具体方法见路由全表 | 已有，GLM 适配已有接口 |
| 报表 | `GET /admin/report/turnoverStatistics`、`ordersStatistics`、`userStatistics`、`top10` | `begin`、`end` 日期 | 已有；含 GLM 新增团购统计，统计口径待对账 |

`/api/admin/**`、`/api/merchant/**`、`/api/partner/**` 当前没有对应控制器。前端不得视为已实现；GLM 优先改接 `/admin/**`，确需薄适配层时由 Codex 按本契约追加。

## 下单约定

自提传 `pickupPointId`、`pickupPersonName`、`pickupPersonPhone`，可传 `pickupTimeSlot`；配送传本人 `addressBookId`。二者互斥，不接受前端自填门店或地址来绕过服务端校验。

商品优先取 `items` 或兼容 `orderDetailList`，每项含 `dishId` 和 `quantity`（兼容 `number`）；不传列表时使用本人购物车。服务端从商品表读取可售状态、价格、库存，合并重复商品，计价并事务扣库存。客户端 `amount`、明细 `price/amount` 不作为成交依据。

目标响应：`{id,orderNumber,orderAmount,amount,orderTime}`，其中两个金额字段暂为兼容别名。目标幂等字段 `clientRequestId` 需要新增 DTO、数据库唯一约束和回放规则；当前基线未实现，不能宣称有效。

## 明确失败行为

- 空订单列表：`total:0,records:[]`，不得返回样例。
- 不存在或非本人订单：失败信封，不生成提货码、不写库、不吞异常。
- 已取消订单：禁止支付；已完成订单：禁止取消；重复模拟支付：沿用原状态和提货码。
- 库存不足/负数量/失效团购：失败并回滚；相关完整业务验证列入后续任务。
- 重复核销：业务失败且不发生第二次履约。
- 真实短信、真实支付退款、评价和营销扩展未完成时明确标为未实现；模拟支付不能表述成真实交易。

变更规则：先更新本文件与状态机，记录影响端及兼容方式，再改实现。状态栏随测试证据更新，不以静态存在替代验证。

## 管理后台接口补充（保留 GLM 已对接路由）

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
| `/admin/report/grouponStatistics`（新增） | GET | `begin/end` 可空（缺省统计全部历史数据；报表页默认传最近 30 天）；data：{ongoing,success,failed,expired,memberCount,grouponOrderCount,grouponTurnover} |


上述管理端补充来自 GLM 的既有契约，Codex 在整合时保留端点与字段。新增的 `/admin/groupon/page` 和 `/admin/report/grouponStatistics` 已有实现，当前完整业务和真实数据库对账仍未执行。团购统计按活动 create_time 统计活动与成员，按订单 order_time 统计订单与已付款金额；`begin/end` 为空时取全部历史，非空为包含边界的日期区间。

整合澄清：报表入参是 `begin/end`；订单退款表示为 `status=6,payStatus=2`，不新增未实现的订单状态 7；核销终态与团购条件仍是后续待修复项，不能把目标契约称作全部已验证。
