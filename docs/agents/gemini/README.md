# Antigravity Gemini 3.8 Flash 交付文档与协作总结

## 1. 角色与职责说明

根据《社区团购系统毕业设计协作方案》与多 Agent 协同契约分工：
- **执行 Agent**：Antigravity Gemini 3.8 Flash
- **职责范围**：
  - 代码基线目录：`front/`（生鲜自提与社区拼团 C 端商城，双端支持 H5 + 微信小程序 mp-weixin）
  - 专属交付目录：`docs/agents/gemini/`
- **任务目标（G1 ~ G6 全栈落实）**：
  - **G1**：双端页面、静态资产、后端 API 基地址配置校验与真机适配。
  - **G2**：商品分类树、商品详情展示、加购交互与浮动购物车角标联动。
  - **G3**：**收货地址簿管理系统**（地址列表、新增、编辑、删除、设为默认；结算页区分门店自提 vs 送货到家）。
  - **G4**：**社区拼团业务闭环**（拼团大厅、开团/参团、详情倒计时、拼团结算与我的拼团记录）。
  - **G5**：下单与模拟支付流程闭环，支持提货码展示与取消订单流转，杜绝前端假成功。
  - **G6**：微信小程序与 H5 双端交互、状态栏胶囊适配与异常弱网降级。

---

## 2. 核心架构与协议对齐

### 2.1 信封协议归一化（解决前后端状态码分歧）
- **现象**：原生苍穹外卖后端以 `code: 1` 标识成功，而前端初始封装严格要求 `code === 200`，导致有效请求被误判为报错。
- **解法**：在 `front/api/index.js` 的 `request()` 基础层进行信封解包兼容：
  ```javascript
  if (res.statusCode === 200) {
    const body = res.data
    if (body && (body.code === 200 || body.code === 1)) {
      return resolve({
        code: 200,
        data: body.data,
        msg: body.msg || body.message || 'success'
      })
    }
    // 错误处理与用户提示
    uni.showToast({ title: body.msg || body.message || '请求失败', icon: 'none' })
    return reject(body)
  }
  ```

### 2.2 履约模式双流转协议（自提 vs 外卖配送）
结算页（`pages/order/confirm`）支持按业务诉求动态切换：
1. **门店自提（Pickup）**：
   - 参数字段：`pickupPointId`（自提点 ID）、`pickupPersonName`（提货人）、`pickupPersonPhone`（手机号）。
   - 履约凭证：生成并展示 6 位自提核销码（`pickupCode`）。
2. **送货到家（Delivery）**：
   - 参数字段：`addressBookId`（地址簿 ID）、`consignee`（收货人）、`phone`、`sex`、`detail`（详细地址）。
   - 自动联动：从地址簿获取默认地址，支持点击一键切换并透传回订单结算页。

---

## 3. 功能交付清单 (G1 ~ G6)

### 3.1 G3 收货地址管理系统
| 模块/页面 | 路径 | 核心能力 |
| --- | --- | --- |
| **API 接口层** | `front/api/index.js` | 封装 `getAddressList`、`getDefaultAddress`、`setDefaultAddress`、`getAddressDetail`、`addAddress`、`updateAddress`、`deleteAddress`。 |
| **收货地址列表** | `front/pages/address/list.vue` | 1. 列表展示（标签：家/公司/学校/默认标识）。<br>2. 快捷设为默认、编辑、删除。<br>3. 支持 `mode=select` 订单选择模式，点击自动选中并返回结算页。 |
| **收货地址编辑** | `front/pages/address/edit.vue` | 1. 收货人、性别单选、手机号格式校验。<br>2. 省市区联动选择器与详细地址输入。<br>3. 快捷标签（家/公司/学校）与默认地址一键开关。 |
| **个人中心联动** | `front/pages/mine/mine.vue` | 在「常用工具」区新增「📍 收货地址」直达入口。 |

### 3.2 G4 社区拼团系统
| 模块/页面 | 路径 | 核心能力 |
| --- | --- | --- |
| **API 接口层** | `front/api/index.js` | 封装 `getGrouponDishList`、`getGrouponDetail`、`openGroupon`、`joinGroupon`、`getMyGroupons`。 |
| **拼团大厅** | `front/pages/groupon/list.vue` | 1. 拼团商品双列流，展示拼团特惠价与单买原价对比。<br>2. 产地直采/鲜度保障服务承诺说明。<br>3. 快捷开团按钮跳转。 |
| **拼团详情** | `front/pages/groupon/detail.vue` | 1. 顶部生鲜商品大图轮播与拼团价格标签。<br>2. 「正在拼的团」实时动态列表与实时倒计时器（HH:MM:SS）。<br>3. 底部双行动栏：原价单买（¥XX） vs 发起拼团（¥XX）。 |
| **拼团结算** | `front/pages/groupon/confirm.vue` | 1. 拼团商品快照与数量步进器。<br>2. 支持「门店自提 / 送货到家」两种履约提货方式。<br>3. 提交订单根据 `grouponId` 自动判断开团（`openGroupon`）或参团（`joinGroupon`）。 |
| **我的拼团** | `front/pages/groupon/my.vue` | 1. 四态 Tab 切换：全部 / 拼团中 / 已成团 / 拼团失败。<br>2. 拼团中卡片支持「邀请好友」一键复制口令。<br>3. 已成团卡片支持「查看提货码」即时弹窗，展示门店自提核销凭证。 |
| **首页快捷入口** | `front/pages/index/index.vue` | 1. 金刚区第 5 宫格「拼团专区」事件直跳拼团大厅。<br>2. 首页右侧悬浮「🔥 拼团」动态胶囊按钮，全屏快速唤起。 |

---

## 4. 路由与页面配置注册

在 `front/pages.json` 中完整注册了 6 个全新业务页面：
```json
{
  "path": "pages/address/list",
  "style": { "navigationBarTitleText": "收货地址" }
},
{
  "path": "pages/address/edit",
  "style": { "navigationBarTitleText": "编辑地址" }
},
{
  "path": "pages/groupon/list",
  "style": { "navigationBarTitleText": "社区拼团大厅", "navigationBarBackgroundColor": "#25B873", "navigationBarTextStyle": "white" }
},
{
  "path": "pages/groupon/detail",
  "style": { "navigationBarTitleText": "拼团详情" }
},
{
  "path": "pages/groupon/confirm",
  "style": { "navigationBarTitleText": "拼团结算" }
},
{
  "path": "pages/groupon/my",
  "style": { "navigationBarTitleText": "我的拼团" }
}
```

---

## 5. 构建与双端验证

1. **H5 构建验证**：
   - 命令：`npm run build:h5`
   - 结果：`DONE Build complete.`（退出码 0，产物生成至 `dist/build/h5`）。
2. **微信小程序 mp-weixin 构建验证**：
   - 命令：`npm run build:mp-weixin`
   - 结果：编译成功，样式及组件模板无任何语法及引用错误。
3. **真实后端连通**：
   - 苍穹外卖 Spring Boot 2.7.3 后端（8080）与 Nginx 反向代理配置正常。
   - 用户端鉴权 Token 统一经由 Request Header `token` 携带，符合后端拦截器规范。

---

## 6. 后续建议与协同移交
1. **与 GLM（管理后台）协同**：
   - 管理端可在新增的 `/admin/groupon/page` 实时查看 Gemini C 端用户发起的拼团状态。
   - 自提点店员可使用扫码/输入核销码接口对 C 端生成的 `pickupCode` 进行状态完结。
2. **与 DeepSeek / Codex 协同**：
   - 若后端新增自动化成团或超时退款定时任务，C 端我的拼团列表将自动拉取状态更新。
