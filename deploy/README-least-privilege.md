# 数据库账号边界

应用运行账号只授予目标库 SELECT、INSERT、UPDATE、DELETE。地址、购物车、商品等功能需要这些权限。初始化和迁移由单独 DBA 账号执行；应用运行时无需 CREATE、ALTER、DROP。

先备份并完成 [部署说明](RUNBOOK.md) 中的初始化或增量迁移，再运行：

```bash
export DB_NAME=cfd_demo
export DB_HOST=127.0.0.1
export DB_PORT=3306
export MYSQL_ROOT_PASSWORD='<DBA环境密码>'
export FRESH_APP_PASSWORD='<随机生成20位以上字母数字口令>'
bash deploy/grant-fresh-app.sh
```

脚本默认创建新账号 cfd_app@localhost。账号已存在时 CREATE USER 失败；不会删除或重置现有账号。密码由环境传入，不进入命令行参数。运行应用时将 DB_USER=cfd_app、DB_PASSWORD 设置为该应用账号口令。

历史 grant-verify.sql 是可选权限探针，需 DBA 先在隔离 cfd_acceptance_* 库创建 seed_probe(id,tag,val)，再用应用账号执行。当前未把这份历史脚本列为本轮已通过证据。真实验证记录以 docs/qa 中最终报告为准。
