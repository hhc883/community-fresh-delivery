# 验收任务接手记录

原方案的 DeepSeek Harness 当前不可用，用户授权 Codex 接手剩余核心工作。测试脚本在独立 worktree/分支实现，经外部 JVM/HTTP 访问真实隔离 MySQL；报告明确标注开发者自测，不冒充其他模型独立审计。

交付路径：tests/acceptance/、docs/qa/、docs/thesis/。当前结果与候选提交见 docs/qa/acceptance-report.md；每次运行的机器报告位于忽略的 .local/acceptance/，包含数据库名称、源码提交、JAR SHA256 和每组结果。不会提交 token、真实凭据、教师联系方式或伪造截图。

用户端和后台构建由同轮前端实现者提交证据。微信设备验证、真实资金与短信、Linux 部署另列未执行，不把程序编译或 H5 浏览器页面等同于微信真机验收。
