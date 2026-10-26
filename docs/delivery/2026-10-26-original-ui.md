# 2026-10-26 原界面恢复交接

用户确认：用户端使用原绿色生鲜自提商城，管理端使用苍穹外卖管理系统。当前工作区 `community-fresh-delivery-codex` 已按此方向修改。2026-10-25 的历史 ZIP 是简化界面，不包含本轮修改。

## 实现

- 从 `origin/develop` 中的原页面恢复绿色首页、分类左右分栏、购物车、个人中心、商品详情、登录胶囊表单与搜索布局。保留绿色头部、轮播卡片、五宫格入口和商品卡片，不恢复旧手机样例商品、假订单、假支付、虚构门店及促销承诺。
- 使用当前请求层和 Vuex 登录态，保留订单幂等意图、服务端计价、真实库存、地址、团购和模拟支付流程。首页自提点入口接真实门店列表；结算继承选择的真实自提点；个人中心订单入口传递实际状态过滤。
- `front-admin-sky/` 是苍穹外卖可编辑 Vue2 原版源码，来源及 MIT 版权见该目录 `UPSTREAM.md` 和 `LICENSE`。保留黄色顶栏、深色侧栏及原工作台、订单、商品、套餐、分类、员工、统计页面，默认同源 `/admin` 接口。
- 现有 `front-admin/` 改为 `/extensions/` 下的业务模块，在苍穹侧栏内同源嵌入，提供拼团、生鲜库存、自提核销和团购报表；共用管理员登录，不传 token URL，不再作为默认独立后台。
- 原商品表单补库存、单位、产地和生鲜标记；拼团参数从“生鲜与库存”管理。上传请求使用真实 `/admin/common/upload`，业务失败不当成上传成功。
- 修复原后台首次连接空 Redis 时 `/admin/shop/status` 空值引发500的问题：未设置默认打烊，设置仅允许0/1。未自动修改营业状态或业务数据。

本轮本地 Antigravity 返回地区不支持API，退出码3，没有实施修改。本轮恢复由 Codex 完成，没有将失败派发算作代理交付。

## 本机运行

已经启动当前源代码编译的版本：

- 用户 H5：http://127.0.0.1:5173
- 苍穹后台：http://127.0.0.1:5177，隔离演示账号 `admin / 123456`
- 后端：http://127.0.0.1:8080
- 微信开发者工具导入：`front/dist/build/mp-weixin`，不是截图中旧 Downloads 目录。

本机保留原数据库 `cfd_demo_manual_20261026`、便携 MySQL 13306、Redis 16379。启动文件 `.local/start-manual-demo.ps1` 已切换为当前源码产物；`.local/stop-manual-demo.ps1` 只停止记录且启动时间匹配的应用进程；`.local/refresh-ui-preview.ps1` 只替换两端预览。文件位于本机忽略目录，不包含在Git或历史ZIP内。

通用后台预览命令在仓库根目录执行：

```sh
python scripts/serve-frontends.py --directory front-admin-sky/dist --extensions front-admin/dist --backend http://127.0.0.1:8080 --port 5177 --spa
```

构建步骤见根 README 和 `deploy/RUNBOOK.md`。生产将苍穹产物放入 `admin/`，业务扩展放入 `admin/extensions/`；不要只发布其中之一。

## 本轮验证范围

- H5、mp-weixin、苍穹管理端、生鲜扩展模块均编译成功。
- 后端使用 `mvn -o -f backEnd/pom.xml -DskipTests package` 打包成功，未执行测试。
- 浏览器只读核对绿色首页、苍穹登录与真实工作台数据，以及侧栏拼团列表。只读确认营业状态接口返回 `code=1,data=0`，不再500。
- 保留实际截图于本机 `.local/ui-restoration/green-home.png`、`sky-dashboard.png`。
- 按用户要求，未重跑交易回归、并发、微信真机及完整手工业务流程；源码构建不代表这些验证完成。原版旧 TypeScript 项目本轮采用转译构建，未完成完整类型检查。CI加入新管理端构建配置，未声称远端CI已执行。
- 本轮没有自动push或merge。

## 微信开发者工具样式编译修复

用户随后反馈 `app.wxss(1:539): error at token '*'`。微信开发者工具自带 `wcsc.exe` 对旧产物复现相同错误并退出1；源码未使用 `.stack`，删除公共样式中遗留的 `.stack > *` 规则。重新生成 mp-weixin 后，原生编译器单独编译 app.wxss 及批量编译全部10个WXSS文件均退出0。uni-app打包成功不能替代微信原生样式编译，本次未声称完成微信真机或业务流程测试。
