# 社区团购与生鲜配送系统

毕业设计核心工程：原绿色 uni-app Vue3 用户端、苍穹外卖 Vue2 / Element UI 管理后台，以及 Spring Boot **2.7.3** 三模块后端、MyBatis / MySQL 8 / Redis。拼团、库存和核销模块接入苍穹后台侧栏。服务默认 `http://127.0.0.1:8080`，业务时间为 Asia/Shanghai。支付、退款和开发验证码均为明确的模拟流程。

[部署与启动手册](deploy/RUNBOOK.md) · [API 契约](docs/contracts/api-v1.md) · [状态机](docs/contracts/state-machine.md) · [需求对应](docs/qa/requirements-mapping.md) · [验收报告](docs/qa/acceptance-report.md) · [论文与答辩素材](docs/thesis/implementation-facts.md)

2026-10-25 已整合验收通过的后端 `1116f99` 与前端 `33c210b`。本轮按用户要求不重新执行测试，完整整合版由用户手工确认；运行步骤和交付边界见 [核心版本交接](docs/delivery/2026-10-25-handoff.md)。

2026-10-26 按用户确认恢复绿色商城界面，并将管理主入口切换为苍穹外卖原版源码。最新目录、运行方式及本轮构建记录见 [界面恢复交接](docs/delivery/2026-10-26-original-ui.md)。旧交付 ZIP 保留作历史记录，不能用它预览本轮界面。

## 快速开始

安装 JDK 17、Maven 3.9.x、Node 22、MySQL 8 和 Redis。配置从环境读取，不复制历史个人设置。

```powershell
mvn -B -ntp -f backEnd/pom.xml verify
# 配置 DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD 后：
python scripts/database.py init --confirm-database cfd_demo
python scripts/database.py migrate --confirm-database cfd_demo
python scripts/database.py seed --confirm-database cfd_demo
java -Duser.timezone=Asia/Shanghai -jar backEnd/sky-server/target/sky-server-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

上例必须显式设置 `DB_NAME=cfd_demo`；init 拒绝已有表的数据库。演示种子含公开的虚构管理员 `admin / 123456`，仅限隔离本地演示。已有业务库应先验证备份，再单独 migrate；不能执行 init 或覆盖历史 SQL。详细环境、应用最小权限账号、Redis 以及生产 profile 见部署手册。

```powershell
cd front
npm ci
npm run dev:h5
# 微信源码构建：npm run build:mp-weixin
cd ../front-admin
npm ci
npm run build
cd ../front-admin-sky
npm ci --legacy-peer-deps --ignore-scripts
npm run build
cd ..
python scripts/serve-frontends.py --directory front-admin-sky/dist --extensions front-admin/dist --backend http://127.0.0.1:8080 --port 5177 --spa
```

用户端 API 地址由构建配置指定，默认 8080；在手机上联调时配置宿主机可访问地址。后台默认开发端口 5177。微信构建与微信真机验收分别记录。

## 验证

后端公开接口/服务回归执行 `mvn -f backEnd/pom.xml test`；Windows 可使用 `scripts/test-backend.ps1` 指定便携 JDK 和 Maven。前端执行 H5、mp-weixin 与管理端构建。

真实 MySQL 外部 HTTP 验收：

```powershell
$env:ACCEPTANCE_ADMIN_PASSWORD='123456'
python scripts/run-acceptance.py
```

该脚本自动创建新的 `cfd_acceptance_*` 数据库，在空闲 18080 端口启动自己的后端，然后验证地址隔离、上下架、服务端价格、库存并发、幂等支付、订单归属、配送、自提、团购和固定样例报表，重启后复查历史订单。它不会连接任意已有应用进行写入，也不包含 DROP/reset。报告记录源码提交、JAR SHA256、版本和结果；依赖端口及命令见部署手册。

[CI](.github/workflows/core.yml) 配置后端测试、MySQL/Redis 外部验收、H5/微信/管理端构建。配置文件存在不代表远端 CI 已执行；当前实际结果、未执行项和设备限制见验收报告。

## 目录与范围

| 目录 | 内容 |
| --- | --- |
| `front/` | 主用户端，H5 / 微信小程序 |
| `front-admin-sky/` | 苍穹外卖原版管理后台源码，默认管理主入口 |
| `front-admin/` | 嵌入苍穹侧栏的生鲜库存、拼团、核销与报表扩展模块 |
| `backEnd/sky-common`、`sky-pojo`、`sky-server` | 公共能力、实体契约、后端实现 |
| `backEnd/sql/` | 无 DROP 当前结构与独立虚构种子 |
| `deploy/`、`scripts/` | 依赖环境、启动、迁移、备份、验收、部署模板 |
| `tests/acceptance/`、`docs/qa/`、`docs/thesis/` | 外部验收及交付事实材料 |
| `front-merchant/`、`front-partner/` | 历史扩展端，未作为核心已交付功能 |

真实短信、微信资金交易、独立商家/自提点权限、骑手端和复杂营销不包含在核心演示。原四 Agent 负责人名称只保留为历史分工；前一批后端由 ZCode、前端由 Antigravity 实现，Codex 负责验收与整合；本轮界面恢复由 Codex 完成，本地 Antigravity 因地区限制未启动实现。原课题文档联系方式不上传。

遵循 [LICENSE](LICENSE)。
