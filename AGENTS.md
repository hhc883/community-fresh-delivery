# 项目开发规则

先阅读 `docs/毕业设计四Agent协作开发方案.md`、`docs/contracts/api-v1.md` 与 `docs/contracts/state-machine.md`。用户于 2026-10-24 已授权按该方案开发；其中“本轮只新增方案”等表述记录的是文档编写时的历史范围，不取消后续开发授权。

- Gemini 主责 `front/` 与 `docs/agents/gemini/`。
- GLM 主责 `front-admin/`、`front-merchant/`、`front-partner/` 与 `docs/agents/glm/`。
- Codex 主责 `backEnd/`、部署、工程脚本、CI、根文档与公共契约。
- DeepSeek 主责 `tests/acceptance/`、`docs/qa/`、`docs/thesis/`、`docs/agents/deepseek/`。
- 不擅自修改其他负责人目录；跨端缺口在公共契约和任务中记录。各开发者使用独立工作树及功能分支。
- 技术基线为 Spring Boot 2.7.3 / MyBatis / MySQL / Redis；不为毕设另建后端或升级 Boot 3。
- 保留 `/api/**` 的成功码 200 与 `/user/**`、`/admin/**` 的成功码 1，使用现有响应适配层。
- 身份取自 JWT；用户操作只允许本人资源；服务端计价；交易修改使用事务与条件更新。
- 测试公开接口与业务结果，不测试私有方法，不以重复实现计算预期值。用户已确认 HTTP 与公开服务接口为 A06–A11 的测试边界。
- 数据写入、重置和并发测试只针对隔离测试库，不对未知业务库执行初始化 SQL。
- 禁止提交真实凭据、用户数据、编译产物、依赖缓存或教师联系方式。测试未执行必须如实记录。

## Agent skills

### Issue tracker

GitHub Issues：`hhc883/community-fresh-delivery`。详见 `docs/agents/issue-tracker.md`。

### Triage labels

使用 `needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`。详见 `docs/agents/triage-labels.md`。

### Domain docs

单上下文：根 `GLOSSARY.md` 与 `docs/adr/`；不存在时继续工作。详见 `docs/agents/domain.md`。

本批来源规格为四 Agent 方案中 Codex C1–C8；首批范围见 `docs/development/backend-baseline.md`。初始规格基线为 `929851efa141ff2c05b9652f23cd4e7b1e091f3a`。整合其他 Agent 提交后，本批差异审查固定点为 `0e6a49c33b2b09c867ee698cf2a604b34dcd7759`，不把保留的其他负责人提交算作本批直接修改。
