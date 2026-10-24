# SiliconFlow AI 对话助手 — 设计文档

**日期**：2026-06-15
**作者**：Brainstorming 流程产出
**状态**：已批准，进入实现规划

---

## 1. 背景与目标

苍穹外卖（sky-take-out）C 端微信小程序当前只有"首页 / 订单 / 我的" 3 个底部 tab。本次新增一个 **AI 对话助手** tab，提供基于硅基流动 SiliconFlow 的多轮对话能力。

### 1.1 本次范围

| 功能 | 优先级 |
|---|---|
| 文本对话 | 必须 |
| 角色消息展示（user/assistant） | 必须 |
| 多轮对话上下文 | 必须 |
| 基本美观 CSS | 必须 |
| 模型选择（Qwen2.5 / DeepSeek / Llama 等） | 可选 |
| 文件/图片处理 | **不在本期** |
| 苍穹外卖业务集成（菜品/订单） | **不在本期**（用户决定先纯聊天） |

### 1.2 非目标

- 不接入苍穹外卖后端业务数据
- 不做用户登录/会话服务端持久化
- 不做语音/图片输入
- 不做 token 计费/限流

---

## 2. 关键决策

| 决策 | 选择 | 原因 / 风险 |
|---|---|---|
| API Key 位置 | **小程序直连 SiliconFlow**（Key 写在 JS 里） | 用户明确要求。**风险**：Key 可被反编译泄露。建议在 SiliconFlow 控制台给 Key 设置月度额度上限作为缓解 |
| 上下文存储 | **小程序 localStorage** | 最简单，零后端改动 |
| UI 入口 | **在 app.json 新增原生 tabBar**，加第 5 个 tab「AI 助手」 | 原项目目前没有 tabBar（app.json 无 tabBar 字段，导航分散在各个页面）。原生 tabBar 是微信官方推荐方案，零运行时开销 |

> 备选方案：若不想动 app.json，也可以在「我的」页面加个入口链接 `/pages/aiChat/chat`（要改 vendor.js 的「我的」页模板），不推荐因为 vendor.js 是编译产物。
| 协议 | OpenAI 兼容协议（POST /v1/chat/completions） | 硅基流动原生支持 |
| 流式响应 | **首选流式**，失败时回退非流式 | 体验更好 |
| 默认模型 | `Qwen/Qwen2.5-7B-Instruct`（免费档够用） | 用户可在设置页改 |

---

## 3. 架构

```
┌─────────────────────────────────────────┐
│  WeiXinApp (uni-app 编译产物)          │
│                                          │
│  pages/index    ━┓                      │
│  pages/order    ━┫   原 3 个 tab         │
│  pages/my       ━┫                      │
│  pages/aiChat   ━┛★ 新增 AI tab         │
│                                          │
│  pages/aiChat/                           │
│   ├─ chat.vue       主聊天页            │
│   └─ settings.vue   配置 API Key / 模型 │
│                                          │
│  common/utils/                           │
│   ├─ siliconflow.js   API 封装          │
│   └─ chatStore.js     localStorage 封装  │
└─────────────────────────────────────────┘
                  │ wx.request
                  ↓
      https://api.siliconflow.cn/v1/chat/completions
```

**无后端改动**——AI 对话完全在小程序端跑。

---

## 4. 数据模型

### 4.1 消息（Message）

```js
{
  id: 'msg_1718352345678_3',   // 时间戳 + 序号
  role: 'user' | 'assistant' | 'system',
  content: '今天吃什么',          // markdown 字符串
  createdAt: 1718352345678,
  pending: true,                 // 流式中（仅 assistant）
  error: null                    // 错误信息（如果有）
}
```

### 4.2 会话（Session）

```js
{
  id: 'session_1718352345678',
  title: '今天吃什么',           // 取首条 user 消息前 20 字
  model: 'Qwen/Qwen2.5-7B-Instruct',
  systemPrompt: '你是苍穹外卖的 AI 助手',
  messages: [Message, Message, ...],
  createdAt: 1718352345000,
  updatedAt: 1718352400000
}
```

### 4.3 设置（Settings）

```js
{
  apiKey: 'sk-xxxxxxxxxxxxxxxx',        // 用户填的 SiliconFlow Key
  defaultModel: 'Qwen/Qwen2.5-7B-Instruct',
  systemPrompt: '你是苍穹外卖的 AI 助手'
}
```

### 4.4 localStorage 键名

| key | value 类型 | 说明 |
|---|---|---|
| `sf_chat_sessions` | `Session[]` | 所有会话 |
| `sf_chat_active_id` | `string` | 当前打开的会话 id |
| `sf_chat_settings` | `Settings` | 用户配置 |

---

## 5. 文件清单

### 5.1 新增文件

```
WeiXinApp/
  pages/aiChat/
    chat.vue              # 聊天主页
    chat.js               # 页面配置
    chat.json             # 页面 json
    chat.wxss             # 样式
    settings.vue          # 设置页
    settings.js
    settings.json
    settings.wxss
  common/utils/
    siliconflow.js        # SiliconFlow API 封装
    chatStore.js          # localStorage CRUD
  static/ai-chat.png      # 「我的」页 AI 助手入口小图标（可选）
```

### 5.2 修改文件

- `WeiXinApp/app.json` — 在 `pages` 数组加 `"pages/aiChat/chat"` 和 `"pages/aiChat/settings"`
- `WeiXinApp/common/vendor.js` — 在「我的」页面渲染逻辑里加一行"AI 助手"列表项（指向 `/pages/aiChat/chat`）
- 不改任何后端代码
- **不动 vendor.js 的现有改动**（之前修登录/修 token 持久化已经改过 vendor.js，本次 AI 助手是新增独立目录，不去碰 vendor.js）

---

## 6. 关键流程

### 6.1 发送消息

```
[用户在 textarea 输入] → [点击发送]
  ↓
[创建 user 消息] → [追加到当前 session.messages] → [持久化]
  ↓
[创建空 assistant 消息（pending=true）] → [追加并持久化]
  ↓
[调用 siliconflow.chatCompletions({ stream: true, onChunk })]
  ↓ 流式 chunk
[onChunk(delta) → append 到 assistant.content] → [setData 重渲染]
  ↓
[onDone] → [pending=false] → [持久化]
  ↓ error
[onError(msg) → assistant.error = msg] → [持久化]
```

### 6.2 切换会话

```
[左侧抽屉点击会话]
  ↓
[activeId = sessionId] → [持久化]
  ↓
[加载对应 session.messages] → [渲染]
```

### 6.3 新会话

```
[右上角 + 点击]
  ↓
[创建空 session（systemPrompt 从 settings 拷贝）] → [插到 sessions 头部]
  ↓
[activeId = 新 session.id] → [持久化]
  ↓
[清空 messages 渲染区]
```

### 6.4 流式响应解析

SiliconFlow 返回 SSE 格式：
```
data: {"id":"...","choices":[{"delta":{"content":"你"},"index":0}]}

data: {"id":"...","choices":[{"delta":{"content":"好"},"index":0}]}

data: [DONE]
```

每次 `data: {...}` 一行，**JSON 解析**出 `choices[0].delta.content` 追加到当前 assistant content。

---

## 7. UI 设计

### 7.1 聊天页（chat.vue）

```
┌────────────────────────────────────┐
│  ≡  AI 助手              +         │  ← 顶栏：左抽屉 + 标题 + 新建
├────────────────────────────────────┤
│                                     │
│  ┌─────────────────────┐            │
│  │ 你好                 │           │  ← 用户消息气泡（右对齐）
│  └─────────────────────┘            │
│            ┌─────────────────────┐  │
│            │ 你好！我是 AI 助手   │  │  ← AI 消息气泡（左对齐）
│            └─────────────────────┘  │
│                                     │
│  [流式输出时显示光标 ▍]              │
├────────────────────────────────────┤
│ ┌──────────────────────────┐  ┌──┐ │
│ │ 在这里输入...              │  │发送│ │  ← 底部输入栏
│ └──────────────────────────┘  └──┘ │
└────────────────────────────────────┘
```

### 7.2 样式要点

- 背景：`#f7f8fa`（浅灰）
- 用户气泡：`#95ec69`（微信绿） / 文字 `#000`
- AI 气泡：`#fff` / 边框 `#e5e5e5` / 文字 `#333`
- 圆角：`8px`
- 间距：`12rpx`
- 输入栏高度：`100rpx`，固定底部
- 字体：system 默认

### 7.3 抽屉（历史会话）

从左侧滑出，列出所有 session（按更新时间倒序），点击切换。底部"清空所有会话"红色文字按钮（弹确认对话框）。

### 7.4 设置页（settings.vue）

三个表单项：
1. **API Key**：input（password 类型），保存到 localStorage
2. **默认模型**：下拉选择（Qwen2.5-7B / Qwen2.5-14B / DeepSeek-V2.5 / 等）
3. **System Prompt**：textarea（多行）

底部"保存"按钮。

---

## 8. 错误处理

| 场景 | 行为 |
|---|---|
| API Key 未填 | chat 页顶部显示提示"请先在设置里填 API Key"，发送按钮禁用 |
| 余额不足 / Key 无效（401/403） | 弹 toast "API Key 无效或余额不足"，assistant 消息标记 error |
| 网络超时（10s） | assistant 消息标记 error（保留已接收内容），用户可点"重试" |
| 流式中断 | 同上 |
| localStorage 满了 (10MB) | catch QuotaExceededError，弹 toast 提示用户清理历史 |
| 系统错误（5xx） | 提示"服务异常，请稍后重试" |

---

## 9. 测试策略

**手工测试为主**（uni-app + 第三方 API 不适合写 e2e 单测）：

| 测试项 | 步骤 | 预期 |
|---|---|---|
| 配置 Key | 设置页填 Key → 保存 → 返回 | 保存成功，localStorage 有值 |
| 发送消息 | 输入"你好" → 发送 | 流式显示 AI 回复 |
| 多轮对话 | 问"1+1" → "再+1" | AI 能基于上下文回答 |
| 新会话 | 点 + | 清空消息列表 |
| 切换会话 | 抽屉选历史 | 加载对应消息 |
| 清空 | 抽屉底部"清空所有" → 确认 | localStorage 清空 |
| 错误 Key | 填假 Key → 发送 | 提示 Key 无效 |

---

## 10. 安全风险（已知并接受）

> ⚠️ **本设计将 SiliconFlow API Key 硬编码到小程序客户端 JS 中**。

### 10.1 风险

1. 任何用户都能用「微信开发者工具」打开小程序代码 → 反编译 `vendor.js` → 拿到 `SK-...` 开头的 Key
2. 拿到 Key 后可以在外部（curl / Python）调用 SiliconFlow API 直到你账户余额耗尽
3. 如果 Key 没设 IP 白名单，从任何地方都能调用

### 10.2 用户已接受的缓解措施（建议但非强制）

- 在 [SiliconFlow 控制台](https://cloud.siliconflow.cn/account/ak) 给 Key 设置**月度额度上限**（如 ¥10/月）
- 定期轮换 Key
- 监控账单，发现异常立刻禁用

### 10.3 生产环境的正确做法（本期不做）

- **后端代理**：sky-server 加 `/api/ai/chat` Controller，前端只传用户消息，后端代调 SiliconFlow
- Key 只存在服务端 `.properties` / 环境变量里
- 后端可加用户级限流、防滥用
- 但本期用户明确要求**小程序直连**，按用户决定执行

---

## 11. 实施计划

进入 `writing-plans` 技能产出实现计划。

预期步骤：
1. 准备 tabBar 图标（2 张 81×81 PNG，浅灰色 AI 文字）
2. 创建 `pages/aiChat/` 两个页面 + 配置文件
3. 创建 `common/utils/siliconflow.js` 和 `chatStore.js`
4. 修改 `pages.json` 加 tab
5. 开发者工具里手动测试 9 项功能
6. 在 README 里加 AI 助手使用说明
