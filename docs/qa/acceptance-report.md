# 核心验收记录

## 核心模块收敛与 Gemini 样式集成

当前这一轮新增本地图片上传/读取公开接口测试4项，后端合计130项通过，失败/错误/跳过均为0。全新隔离数据库 `cfd_acceptance_20261026_231528_d2af53` 的14组真实MySQL HTTP场景通过；另外实际HTTP确认上传鉴权、无效图片拒绝、有效PNG上传读取及商品公开展示。前端6项结算行为测试通过。

浏览器对同一隔离后端实际操作了：验证码登录、地址新增/默认地址、普通配送购买付款/接单/派送/完成、自提付款/核销，以及两位独立用户开团/参团/付款/成团。上传控件实际选择本地PNG，保存后重新读取商品列表。合并后的销量与团购报表加载真实数据。

本地Gemini在独立工作树修改9个样式文件；Codex对照派发基线验证模板与脚本未作语义修改，并仅提取样式到当前业务版本。H5、mp-weixin、生鲜扩展与苍穹管理壳构建成功，微信开发者工具自带wcsc编译全部10个WXSS文件成功。完整TypeScript类型检查、微信真机和真实第三方支付/短信不在这些通过项中。新JAR与开发服务运行副本分离，完整本机启动已实际验证。

证据为本机 `.local/core-module-maven-final.log`、`.local/acceptance/core-modules-final-20261026.json`、`.local/core-browser-fixtures.json`、`.local/gemini-design-integrated.json` 与 `.local/core-evidence/`；这些含虚构联调数据的运行产物没有上传Git。后续源码提交以本交接文件所属提交为准，不把旧来源提交号当成本轮测试版本。

## 最终来源版本记录（2026-10-25）

后端候选 `1116f99`：Codex 独立 `mvn clean package` 126项测试、14项真实MySQL HTTP验收、2项缺陷补充探针全部通过；其中旧支付做5轮×8并发回包核对，商品负拼团价部分更新被拒且原值保持。隔离库为 `cfd_acceptance_20261025_212815_42fe9a`，JAR SHA256为 `268b15c7a1264965b5ad56423f39da6fbbd150539e769c9b71882e9b44f9738e`。

前端候选 `33c210b`：6项公开行为测试、2项独立缓存/数字履约检查及H5、微信、管理端三个构建通过。登录、新用户空订单、退出与管理商品回填/非法名称反馈做过实际浏览器检查；未据此声明所有浏览器业务路径均通过。

本次将两个来源合入 `feat/graduation-core-completion`，不重新执行测试；用户负责最终手工确认。微信真机、真实短信/资金及真实部署实测仍未执行或未开放。完整来源、运行步骤与边界见 [交接记录](../delivery/2026-10-25-handoff.md)。以下初轮失败保留为历史，不能作为当前未修复清单。

## 历史初轮

记录日期：2026-10-25，Asia/Shanghai。当前为开发者自测与外部进程 HTTP 验收，不声称由不可用的外部 Agent 独立签字。最终候选回归后更新本记录。

## 已执行环境

- Microsoft OpenJDK 17.0.20.1、Maven 3.9.9；保留 Boot 2.7.3。
- 官方便携 MySQL Community 8.0.44，127.0.0.1:13306；使用全新工作树 `.local/mysql-data`，未接触未知业务库。
- redis-windows 8.10.2 社区 Windows 构建，127.0.0.1:16379，实际 PING 返回 PONG；这是社区发行，不称为官方 Windows Redis。
- 后端独立进程 18080。初轮测试代码提交 `a1374ef54b3f1c9089c6ede1fde4ec7433a9e4dc`，JAR SHA256 `513b01b802f601e7611ba8a707718781b487a482a65dd30baf01bb010aeb2f33`。

本地便携依赖来自 [MySQL 官方下载](https://cdn.mysql.com/Downloads/MySQL-8.0/mysql-8.0.44-winx64.zip)、[Redis Windows 项目发布](https://github.com/redis-windows/redis-windows/releases/tag/8.10.2)、[Node 官方发行](https://nodejs.org/dist/v22.14.0/)。运行时、缓存和日志均未提交。

## 数据库与初轮 HTTP 结果

当前结构从空库初始化成功，m21/m22/m23 执行后重复执行成功。另从历史 SQL **仅提取 14 个 CREATE TABLE**，在新的 `cfd_acceptance_migration_20261025_051606` 导入虚构数据后两次迁移成功。迁移前后两条虚构订单的 id/number/status/payStatus/amount 完全一致，三种订单唯一索引存在；再次 init 被拒绝。没有执行历史 DROP 或真实样例导入。

初轮 HTTP 数据库：`cfd_acceptance_20261025_051428_595d89`。使用 Python 标准库经 HTTP 调用，业务断言没有查询数据库或导入后端实现。端口空闲检查、脚本启动独立后端与随机新库保证写入隔离。结果为 13 组中 **12 通过、1 失败**：

| 用例 | 结果 | 实际观察 |
| --- | --- | --- |
| A01 初始化/迁移/启动 | 通过 | 当前结构、历史结构升级及重复迁移成功 |
| A02 地址 | 通过 | 默认地址可读，其他用户不可读/删除 |
| A03 商品管理 | 通过 | 上下架及库存修改影响下单 |
| A04 两人成团 | 通过 | 两用户参团达到 status=2/currentSize=2 |
| A05 到期/重复参团 | 通过 | 到期单 status=6/payStatus=2，库存只回补一次；重复参团拒绝 |
| A06 改价与数量 | 通过 | 客户端 0.01，后端两件商品成交 25.00；负数/零/超库存拒绝 |
| A07 幂等/并发 | 通过 | 相同请求返回同一订单；八用户争一件只有一人成功；取消后库存恢复 |
| A08 归属与真实性 | 通过 | 新用户空记录；他人及不存在订单失败 |
| A09 支付 | 通过 | 模拟支付重试码相同；取消后支付失败；终态不退回待履约 |
| A10 履约 | 通过 | 非法跳转拒绝；配送推进完成；重复核销失败 |
| A11 权限 | 通过 | 用户与管理员 JWT 不能互换 |
| A12 报表 | **失败，待复测** | 两天范围只返回首日；37.50 金额正确但缺第二天 0 值 |
| A13 微信真机 | **未执行** | 需要开发者工具/设备及合法 AppID/网络配置 |
| A14 重启 | 通过 | 重启独立 JVM 后本人已完成订单仍为 status=5 |

实际命令为 `mvn -o -B -ntp -f backEnd/pom.xml package -DskipTests` 和 `python scripts/run-acceptance.py --keep-running --report .local/acceptance/initial-red.json`；JDK/Maven/MySQL/Python 使用便携路径，依赖缓存复用项目 `.local/m2`。初次沙箱构建受依赖 JAR 访问权限限制失败，经允许的构建执行环境重跑成功；这与业务失败分别记录。

`.github/workflows/core.yml` 已配置后端公开接口测试、真实 MySQL 迁移及 HTTP、H5/微信/后台构建。远端 GitHub CI、Docker compose、systemd/Nginx、真实微信资金、短信和微信真机尚未执行，不能从配置存在推断通过。
