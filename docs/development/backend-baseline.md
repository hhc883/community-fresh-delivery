# 后端开发基线与需求对应

日期：2026-10-24；来源提交 `929851efa141ff2c05b9652f23cd4e7b1e091f3a`。

- origin：`https://github.com/hhc883/community-fresh-delivery.git`。已通过 `git ls-remote --heads origin` 只读确认远端仅有 main，且与来源提交一致；已认证核验仓库 push 权限；实际功能分支推送和 PR 另记交付结果。
- Codex 独立工作目录：`community-fresh-delivery-codex`；分支 `feat/codex-backend-foundation`。原工作区的协作方案为用户未追踪文件，保留原件并复制至本工作树。
- 后端：Boot 2.7.3；`sky-common`、`sky-pojo`、`sky-server`；MyBatis、MySQL、Redis；默认 8080。
- 当前环境 PATH 没有 Java、Maven、MySQL、Redis、Docker 或 gh；原 build-run.bat 引用的本地 JDK/Maven 路径不存在。已在工作区准备 Microsoft OpenJDK 17.0.20.1 和 Maven 3.9.9，未修改系统 PATH。
- 尚未启动真实 MySQL/Redis；没有运行或重置现有业务数据库。单元/隔离数据库测试与真实 MySQL 端到端验收必须分开报告。
- 用户端开发 API 指向 8080；其他前端仍指向 3001 且调用当前不存在的 API。前端目录由其负责人维护，具体交接见 API 契约。

| 需求/风险 | 现有模块 | 任务 | 验收 |
| --- | --- | --- | --- |
| 可重复启动与接口基线 | Maven、配置、部署与 README | C1、C2、C8 | A01、A14 |
| 本人地址隔离 | AddressBookService | C3、C6 | A02、A11 |
| 商品上下架和库存 | DishService/Mapper | C4、C6 | A03、A06、A07 |
| 团购与到期回收 | GrouponService | C5 | A04、A05 |
| 服务端金额与统一下单 | OrderService、ApiOrderController | C4 | A06、A07 |
| 空列表、无效/他人订单 | 用户订单 API | C3 | A08、A11 |
| 支付/取消/核销状态 | OrderService | C3、C4 | A09、A10 |
| 真实报表 | ReportService | C6 | A12 |
| 小程序真机 | Gemini 用户端 | 外部依赖 | A13 |

首批交付目标：路由/角色/状态契约、移除示例订单和无订单提货码、用户订单及地址归属校验、设置密码鉴权和请求上下文清理。后续按独立可验证任务实现计价库存、团购和履约，不能将第一批描述为完整毕设已交付。

## 已登记的 GitHub 工作

父规格 [#1](https://github.com/hhc883/community-fresh-delivery/issues/1)，子任务：[#2 基线与契约](https://github.com/hhc883/community-fresh-delivery/issues/2)、[#3 认证与归属](https://github.com/hhc883/community-fresh-delivery/issues/3)、[#4 计价库存幂等](https://github.com/hhc883/community-fresh-delivery/issues/4)、[#5 团购履约统计](https://github.com/hhc883/community-fresh-delivery/issues/5)、[#6 初始化部署和集成](https://github.com/hhc883/community-fresh-delivery/issues/6)。已建立父子与原生 blocking 关系，未关闭父规格。
