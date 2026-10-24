# GLM 工作记录：管理后台接通真实后端（L1–L6）

日期：2026-10-24。执行人：GLM 5.3 / Flash（ZCode 会话）。
依据：`docs/毕业设计四Agent协作开发方案.md` §3.2 任务包；用户批准范围为「GLM 分工为主 + 必要后端支撑」。

## 结论摘要

- `front-admin/` 原来是对着旧备份后端 `backEnd_sb3_backup/`（com/fresh，3001）写的，其引用的
  `/api/admin/v2/*`、`/api/admin/auth/login`、评价审核、入驻审核、审计日志等接口在真实基线后端
  `backEnd/`（sky，8080）中**不存在**。本次按方案 L1「按冻结契约替换不存在的接口调用」完成对接。
- 冻结契约：`docs/contracts/api-v1.md`、`docs/contracts/state-machine.md`（M0 前置件，供其余 agent 与
  DeepSeek 验收使用；契约正文由 GLM 代拟，Codex 复核后如有修订以 Codex 版为准）。
- 后端必要支撑（薄适配，核心 service 保持一份）：仅新增
  `GET /admin/groupon/page`（拼团活动分页）与 `GET /admin/report/grouponStatistics`（拼团统计），
  复用既有 `/admin/**` 全部接口与员工 JWT 鉴权。`mvn compile` 通过。
- `front-admin/` 重写请求层与五个页面；删除三个无后端数据支撑的页面（评价审核、入驻审核、审计日志），
  对应 L6「否则关闭未实现入口」。

## 修改清单

| 目录 | 内容 |
| --- | --- |
| `docs/contracts/` | 新增 api-v1.md、state-machine.md（契约冻结） |
| `backEnd/sky-pojo` | 新增 `GrouponPageQueryDTO`、`GrouponStatisticsVO` |
| `backEnd/sky-server` | `GrouponMapper`(+XML) 增 pageQuery/statistics；`GrouponService/Impl` 增 getAdminPage；`ReportService/Impl` 增 getGrouponStatistics；admin `GrouponController` 增 /page；`ReportController` 增 /grouponStatistics |
| `front-admin/src/api/index.js` | 重写：BASE_URL 指向 8080；信封 code===1 / HTTP 401 分流；全部接口函数对齐契约 |
| `front-admin/src/views` | Overview（workspace 三接口）、Dishes（分类+商品 CRUD+库存+团购字段）、Orders（条件搜索+详情+状态流转+提货码核销）、GrouponPage（新）、Reports（新，5 类统计）为真实数据；删除 Reviews/StoreApplies/AuditLogs |
| `front-admin/src/router/index.js`、`App.vue`、`vite.config.js` | 路由与导航同步，移除失效注释 |

## L1–L6 对照

- **L1 登录与构建**：登录走 `POST /admin/employee/login`（员工 JWT，Authorization: Bearer）；
  `npm run build` 通过（产物见构建日志）。401 统一清态回登录页，业务失败 `code=0` 原样透出后端 message。
- **L2 商品**：分类增删改 + 商品增删改查 + 起售/停售 + 库存直接置值 + 团购参数（拼团价/成团人数/有效时长），
  全部走真实 DishDTO 字段，非只读列表。
- **L3 团购与履约**：拼团活动分页（新接口）、手动校验成团（幂等）、一键关闭超时（服务端处置订单）；
  订单条件搜索、详情、接单/拒单/取消/派送/完成、提货码核销（重复核销由服务端 verify_status 拒绝）。
- **L4 统计**：营业额趋势、订单趋势、新增用户、销量 Top10（区间可筛，默认近 30 天）+ 新增拼团统计；
  每块都标注了统计口径。
- **L5 对账**：待真实数据库运行后进行（本会话未启动 MySQL/Redis，运行时验证未执行，见下方「未验证」）。
- **L6 扩展端**：`front-merchant/`、`front-partner/` 请求的 `/api/merchant/**`、`/api/partner/**`
  在真实后端无映射，本版未接通；各端目录已加 README 状态说明（未实现入口如实标注，不参与本次验收）。

## 未验证 / 阻塞项（如实记录）

- 未启动 MySQL/Redis/后端服务，未做端到端联调；A01（空库初始化）、A12（统计对账）等验收用例待
  环境具备后执行，DeepSeek 可按 `docs/contracts/api-v1.md` 直接编写验收脚本。
- 商品图片：库内存量图片路径多为 `/static/...`（用户端静态资源），管理端仅原样展示；
  新增/编辑时建议填可访问 URL 或走 `/admin/common/upload` 上传（上传通道已有，本版页面用 URL 输入框）。
- 拼团成员明细：管理端分页接口不带成员列表（避免 N+1），成员弹窗为如实占位；
  如需管理端查看成员，需 Codex 评估增加只读端点（不阻塞核心闭环）。
- 员工账号：`employee` 表初始数据以 `sky_take_out.sql` 为准，登录用户名/密码见该初始化脚本（不在此复述）。

## 遗留建议（交 Codex / DeepSeek）

1. `docs/contracts/api-v1.md` 请 Codex 复核并接管维护（属 Codex 主责的公共 docs）。
2. 拼团统计的「订单/金额按 order_time 过滤」与「活动数按 create_time 过滤」两个口径，请在报表对账
   用例（A12）中显式覆盖。
3. 方案 §2.1 列出的后端优先问题（示例订单兜底、服务端计价、归属校验等）不在本次范围，仍需 Codex 处理。
