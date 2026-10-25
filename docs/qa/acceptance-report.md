# 核心验收记录

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
