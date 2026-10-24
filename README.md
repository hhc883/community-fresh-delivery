# 社区团购与生鲜配送系统

毕业设计开发中的项目。当前真实技术基线是 **Spring Boot 2.7.3 + MyBatis + MySQL + Redis**，用户端为 uni-app Vue3，管理端为 Vue3 / Element Plus。

既有页面与接口仍在补齐和验证，不能据文件存在认定功能完成。真实微信支付、短信和复杂营销不属于首期交付；现有支付入口为模拟支付。开发 profile 的手机号登录使用明确标识的模拟验证码（默认 123456），可用 AUTH_DEV_CODE 覆盖；对外部署必须设置 AUTH_MOCK_LOGIN=false，并关闭已有微信 mock-login。未配置短信服务时不返回“已发送”。

## 协作与契约

- [四 Agent 协作开发方案](docs/毕业设计四Agent协作开发方案.md)：职责和 A01–A14 验收。
- [API v1](docs/contracts/api-v1.md)：路由、成功码、角色、金额与兼容约定。
- [订单与团购状态机](docs/contracts/state-machine.md)。
- [现有路由清单](docs/contracts/interface-inventory.md)：静态提取，不代表运行验收通过。
- [后端基线与需求对应](docs/development/backend-baseline.md)。

## 目录

| 目录 | 内容 | 主责 |
| --- | --- | --- |
| `front/` | uni-app 用户端，H5 / 微信小程序 | Gemini |
| `front-admin/` | 平台管理后台 | GLM |
| `front-merchant/`、`front-partner/` | 扩展运营端，核心闭环后再启用 | GLM |
| `backEnd/sky-common` | 公共异常、JWT、结果和工具 | Codex |
| `backEnd/sky-pojo` | 实体、DTO、VO | Codex |
| `backEnd/sky-server` | 服务、控制器、MyBatis 和后端内部测试 | Codex |
| `deploy/`、`scripts/`、`tools/` | 部署和工程脚本 | Codex |
| `tests/acceptance/`、`docs/qa/`、`docs/thesis/` | 独立验收和毕设素材，按方案逐步增加 | DeepSeek |

历史 `backEnd/WeiXinApp/` 与 `backEnd/Web/` 暂作参考，主用户端是 `front/`。

## 后端环境与构建

建议 JDK 17、Maven 3.9.x、MySQL 8 和 Redis。项目保留 Boot 2.7，不按旧 README 中的 Boot 3 路径启动。

```powershell
mvn -f backEnd/pom.xml test
mvn -f backEnd/pom.xml package
java -Duser.timezone=Asia/Shanghai -jar backEnd/sky-server/target/sky-server-1.0-SNAPSHOT.jar
```

Windows 可显式指定便携工具目录，不依赖旧 build-run.bat 的个人路径：

```powershell
./scripts/test-backend.ps1 -JavaHome '<JDK目录>' -MavenHome '<Maven目录>'
# 单个回归测试：增加 -Test 'BackendContractTest#测试名'
```

该脚本把 Maven 缓存放在忽略的 `.local/m2/`。测试使用隔离 H2，只证明相应后端行为；真实 MySQL 初始化、跨端联调和小程序真机另行验收。

默认后端地址 `http://localhost:8080`，接口文档 `/doc.html`。配置位于 `backEnd/sky-server/src/main/resources/application*.yml`。现有 dev 配置含历史个人设置，应在本地以环境变量或私有配置覆盖，不能复制到生产。

## 数据初始化

`backEnd/sky_take_out.sql` 含 **DROP TABLE**，只可用于明确的空开发/测试数据库，不可反复导入已有业务库。

已有数据库的增量脚本位于 `backEnd/sky-server/src/main/resources/db/`。基线已含部分自提字段；m21/m22 有各自的字段检查与固定数据库名，执行前核对目标库和结构。当前仍待完成干净 MySQL 环境的顺序验证，不能将这段说明当作初始化验收通过。

## 前端运行与联调

```powershell
cd front
npm ci
npm run dev:h5
npm run dev:mp-weixin
```

管理端在 `front-admin/` 使用自身 package.json 的命令。各端构建与微信真机验证由对应负责人提供证据。

用户端开发配置已指向 8080。管理、商家、自提点端仍有 3001 和不存在的 `/api/admin/**` 等旧请求，需要负责人按 API 契约改接真实后端。旧 `scripts/smoke.sh` 包含与现有工程不符的假设，不能作为当前验收通过依据。

## 当前开发次序

先核实基线与契约，再修复认证、示例数据和订单归属；随后完成服务端计价、事务库存、幂等、团购、履约、报表和部署。各项完成后记录实际命令与结果；未执行、失败、阻塞均保留真实状态。

遵循 [LICENSE](LICENSE)。
