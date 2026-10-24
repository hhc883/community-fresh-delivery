# Engineering skills 配置草案

依据：毕业设计四 Agent 协作开发方案，Codex 负责后端与集成。

建议配置：
- Tracker：GitHub Issues，仓库 hhc883/community-fresh-delivery；按功能 PR 集成。
- 标签：needs-triage、needs-info、ready-for-agent、ready-for-human、wontfix。
- 项目规则入口：新建 AGENTS.md，保留原四 Agent 目录职责。
- 领域文档：单上下文，根 GLOSSARY.md + docs/adr/；不存在时继续工作。
- 首批任务：01 基线与契约（无依赖）；02 认证与订单真实性（依赖 01）；03 服务端计价与库存（依赖 01、02）；04 团购和履约（依赖 03）；05 部署与集成（依赖 02、03、04）。每项按可验证业务行为再细分。
- 测试边界：HTTP 入口的认证、响应与访问隔离；公开 OrderService 接口的归属、金额、状态和幂等行为。沿用方案 A06–A11，先失败再修复。
- 审查基线：初始提交 929851e。

拟写入 AGENTS.md 的 Agent skills 配置：

## Agent skills

### Issue tracker

GitHub Issues，详见 docs/agents/issue-tracker.md。

### Triage labels

使用上述五个默认标签，详见 docs/agents/triage-labels.md。

### Domain docs

单上下文 GLOSSARY.md 与 docs/adr/，详见 docs/agents/domain.md。

备选：本地 Markdown tracker，将任务存为 .scratch/graduation-backend/issues/ 下逐任务文件；其他配置相同，发布 PR 前核对远端与授权。
