# 可重复运行与部署

技术基线：JDK 17、Maven 3.9.x、Spring Boot 2.7.3、MySQL 8、Redis、Node 22。默认后端为 `127.0.0.1:8080`。用户前端和后台独立构建；真实微信支付、短信、商家及自提点独立端不在核心演示范围。

Windows本机已有便携依赖时，构建后运行 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1 -Database cfd_demo`，停止使用 `scripts/stop-local.ps1`。脚本参数支持 `-RuntimeRoot`、`-PythonBin`、`-JavaBin`；默认识别同级便携依赖目录，Python需3.10以上。现有演示数据请明确使用原库名 `cfd_demo_manual_20261026`，脚本会保留已有表，不重复初始化或种子。

本地上传默认写入应用工作目录 `.local/uploads`，可通过 `APP_UPLOAD_DIR` 指定持久目录。后台只接受不超过2MB的PNG/JPEG，返回 `/uploads/` 地址；前端代理和Nginx需保留该路径。备份时同时保留数据库与图片目录。无需配置云存储账号即可完成商品图片上传。

## 构建

```powershell
mvn -B -ntp -f backEnd/pom.xml verify
cd front
npm ci
npm run build:h5
npm run build:mp-weixin
cd ../front-admin
npm ci
npm run build
cd ../front-admin-sky
npm ci --legacy-peer-deps --ignore-scripts
npm run build
cd ..
```

后端可执行文件是 `backEnd/sky-server/target/sky-server-1.0-SNAPSHOT.jar`。H5 产物位于 `front/dist/build/h5`，微信产物位于 `front/dist/build/mp-weixin`，苍穹管理端位于 `front-admin-sky/dist`，业务扩展位于 `front-admin/dist`。微信构建成功仅证明源码编译，不代替开发者工具和真机验证。

本机预览后台：`python scripts/serve-frontends.py --directory front-admin-sky/dist --extensions front-admin/dist --backend http://127.0.0.1:8080 --port 5177 --spa`。访问 `http://127.0.0.1:5177`，由苍穹侧栏进入拼团、库存、自提核销和团购报表。两个管理端构建必须同时部署在同一个域名下。

## 数据库：初始化、迁移、种子分别执行

`scripts/database.py` 使用 Python 标准库和 MySQL CLI。凭据仅从环境传入，未使用命令行密码。先设定明确的目标，不默认选择旧个人数据库。

```powershell
$env:DB_HOST='127.0.0.1'
$env:DB_PORT='3306'
$env:DB_NAME='cfd_demo'
$env:DB_USER='<初始化账号>'
$env:DB_PASSWORD='<本地环境密码>'
# 若 mysql 不在 PATH：$env:MYSQL_BIN='<mysql.exe绝对路径>'
python scripts/database.py init --confirm-database cfd_demo
python scripts/database.py migrate --confirm-database cfd_demo
# 仅本地演示需要：包含公开的虚构管理员 admin / 123456，禁止对外使用。
python scripts/database.py seed --confirm-database cfd_demo
```

`init` 只在目标库没有任何表时执行；发现已有表立即拒绝。当前完整结构为 `backEnd/sql/001-core-schema.sql`，没有 DROP 和固定 USE。`seed` 仅允许 `cfd_demo*`、`cfd_acceptance_*`、`cfd_ci*`，不会随应用启动自动执行。样例商品、人员、地址均为虚构；演示苹果单价 12.50、团购价 10.00、两人成团。初始库存 99，有一件属于待关闭到期团的预留库存；首次到期回收后为 100。

**已有数据库先做并验证备份，只执行 migrate。** 它依次执行 m21/m22/m23，兼容旧脚本中的固定 `USE sky_take_out` 时仅在进程内移除该语句，始终选择明确确认的目标库。m23 添加幂等字段、订单号/核销码/成员唯一约束和库存预留表。历史重复订单号、核销码或成员会使唯一索引创建失败：必须先人工核查，脚本不会清洗、覆盖或删除旧数据。MySQL DDL 不保证整批事务回滚，中断后先核对再重试。旧 `backEnd/sky_take_out.sql` 含 DROP，只作历史参考，不能导入已有业务库。

可选 Docker 依赖：先设置 `MYSQL_ROOT_PASSWORD` 与 `DB_PASSWORD`，执行 `docker compose -f deploy/compose.yml up -d`。MySQL 映射 13306，Redis 映射 16379，均只绑定本机；数据使用持久卷。该 compose 只启动依赖，初始化仍需显式命令。Windows 本轮使用便携 MySQL 与 Redis，未据此声称 Docker 已运行。

## 启动与本地验收

```powershell
$env:DB_USER='<应用账号>'
$env:DB_PASSWORD='<应用密码>'
$env:REDIS_HOST='127.0.0.1'
$env:REDIS_PORT='6379'
$env:AUTH_MOCK_LOGIN='true'
$env:AUTH_DEV_CODE='123456'
java -Duser.timezone=Asia/Shanghai -jar backEnd/sky-server/target/sky-server-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

开发模拟验证码明确标识，不发送短信；支付是模拟资金状态。手机或微信开发者工具访问宿主机时，需要将 `SERVER_ADDRESS` 设为可访问网卡地址，并在前端构建时配置真实 API 地址。默认回环地址仅供本机使用。

外部 HTTP 验收不连接一个任意已有服务。它确认端口空闲、自动创建新的 `cfd_acceptance_日期_随机数` 数据库、运行初始化/迁移/种子、启动独立后端，再执行公开接口场景。数据库保留供排查，没有 reset 或 DROP 操作。

```powershell
$env:ACCEPTANCE_ADMIN_PASSWORD='123456' # 只对应上面的虚构隔离种子
# 依赖端口按本地环境设置；便携环境 DB_PORT=13306 REDIS_PORT=16379
python scripts/run-acceptance.py
# 保留启动的服务以便浏览器联调：增加 --keep-running
```

默认报告 `.local/acceptance/report.json` 记录源码提交、JAR SHA256、结果及隔离数据库；同目录服务器日志可能含虚构测试请求，不应作为公开用户数据。并发用八名用户争抢最后一件；报表断言独立样例 2000-01-01 完成 3 件、37.50 元。程序重启后再从 HTTP 读取本人订单，检查持久化。未运行的设备验证单独保留状态。

## Linux 部署

1. 建立专用 `fresh` 系统用户，将 JAR 放入 `/opt/community-fresh-delivery/`；后端连接已迁移 MySQL 和 Redis。
2. 由 DBA 使用 `deploy/grant-fresh-app.sh` 创建新的应用账号，只授予该库 CRUD。脚本不会 DROP USER；已有账号须由 DBA 单独审核。
3. `/etc/community-fresh-delivery.env` 仅允许服务账号和管理员读取。至少设置 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`、`REDIS_HOST/REDIS_PORT`、`JWT_ADMIN_SECRET/JWT_USER_SECRET`；两个 JWT 密钥应随机生成且不同。Redis 需要密码时另设 `REDIS_PASSWORD`。
4. `deploy/fresh.service` 使用 `prod`，该 profile 禁用手机模拟和微信模拟，缺少 DB/JWT 配置时启动失败。真实短信未接入时不能使用短信登录。需要微信登录/OSS 时再配置相应环境变量。
5. 将用户 H5 放入 `user/`、苍穹管理端 `front-admin-sky/dist` 放入 `admin/`、扩展模块 `front-admin/dist` 放入 `admin/extensions/`。用户前端 API 基址配置为真实后端或同源代理；苍穹管理端使用同源 `/admin`，扩展模块生产构建设置 `VITE_API_BASE_URL` 为空字符串以使用同源接口。`deploy/nginx-fresh.conf` 给出两个域名的托管示例；替换域名并配置真实 TLS 后，先 `nginx -t` 再加载。
6. 安装 systemd unit，`systemctl daemon-reload`、`systemctl enable --now fresh`，通过日志与真实 API 检查连接和权限。systemd/Nginx 为部署模板，本机 Windows 没有执行这些 Linux 命令。

`deploy/backup.sh` 需要显式 DB 和备份目录，生成唯一时间戳备份并检查 gzip；不自动删除备份。恢复必须先在新隔离数据库演练，禁止把演示初始化覆盖到已有业务库。
