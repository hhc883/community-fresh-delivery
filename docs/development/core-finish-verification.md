# 核心收尾真实验证记录（ZCode，2026-10-25）

最新复验以 `1116f99` 为准：R1旧支付并发回包、R2部分更新拼团校验已修复；Codex独立干净重建126项测试、14项HTTP与2项缺陷探针全部通过。前端 `33c210b` 的6项测试和三个构建通过。下面124项为修复前一轮的历史记录。最新交付和手工确认范围见 [交接记录](../delivery/2026-10-25-handoff.md)。

工作树 `community-fresh-delivery-zcode`，分支 `feat/zcode-core-finish`。接手三个原工作树的 26 个暂停文件
（`handoff-manifest.json` 全部 SHA 校验通过，dirty 内容保留并在此基础上继续），在本批完成收尾。

## 命令与结果

- `mvn -Dmaven.repo.local=<codex m2 缓存> package`（JDK 17.0.20.1，Maven 3.9.9）：
  **124 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS**。
  基线 97 项断言全部保留，新增 27 项（商品管理校验 7、门店/用户中心/大盘等含在 Catalog 18 项与团购 18 项内）。
- `python scripts/run-acceptance.py --port 18081 --report .local/acceptance/report-zcode.json`
  （隔离库 `cfd_acceptance_20261025_172814_885be4`，MySQL 13306 / Redis 16379 / SKY_SCHEDULING_ENABLED=false）：
  **14/14 PASS**，含 SETUP、A02–A12、A05-CANCEL、A14。脚本在同一库重复执行 migrate 验证幂等，重启后校验订单持久化。

## 本批修复的缺陷（均先有失败测试再实现）

1. 门店接口编造假数据：空列表注入「高黎柱发士多店」兜底、distance 无值时编造 `260m`、service 无值编造
   「支持冷冻/冷藏」（`PickupPoint` getter 假默认值）。改为只回数据库真实值；新增 `GET /api/stores/{id}`
   详情，不存在或停用明确 400。
2. 商品管理无校验：新增必填名称、有效分类（外键存在）、价格>0、库存非负、拼团参数（拼团价>0、人数≥2、
   时长>0）；创建与口味同一事务（口味插入失败回滚商品）；修改/删除不存在对象明确失败；删除前检查活跃订单
   库存预留（order_stock join orders status in (1,2,3,4)）。后台分页移除强制 `pageSize=100000`，`isFresh` 保留。
3. Workspace 大盘把未付款的已完成订单计入营业额与有效订单：统一为 `status=5 AND pay_status=1`；
   用户统计 SQL 边界从 `>` 改为 `>=`（含当日 00:00:00），与订单统计一致。
4. 并发重复模拟支付返回空提货码：`mockPay` 回查改 `getByIdForUpdate` 当前读，避免 REPEATABLE_READ
   旧快照读到 `pickupCode=NULL`。HTTP 验收 8 并发支付现返回同一提货码。
5. `GlobalExceptionHandler` 把数据库原始报错（表/列名）回传前端：改为只进日志，对外统一「未知错误」；
   类型不匹配/缺参/不可读 JSON/非法参数统一 400 明确契约。
6. 用户中心：未持久化用户 400；`orderCount` 为真实分页计数；头像只更新本人 avatar 字段，
   请求中的 `password`/`userId` 被忽略。
7. 未实现能力（评价、秒杀、优惠券、邀请、提现、免费团、换绑手机号）明确 HTTP 501 `{code:501}`。

## 验收脚本校准（依据已提交契约，非降低断言）

`terminal_admin_rejections` 中对已取消订单的重复取消：`state-machine.md` 明文「重复取消可幂等返回」，
契约测试 `administratorCancellationRecordsMockRefundAndReleasesStockOnce` 断言重复取消 code=1 且库存/退款
只处理一次。验收脚本原断言「必须拒绝」与上述两处冲突；已改为断言**终态与 payStatus 不变**（无论响应码），
终态矩阵强度不变。

## 未执行 / 未开放项

- 微信真机、真实短信、真实微信支付与退款、商家独立角色：不具备条件，未验证（与前批一致）。
- front-merchant / front-partner 为未开放占位端（无 package.json），不参与构建与验收；CI 三个前端构建
  （front H5、front 小程序、front-admin）在 `.github/workflows/core.yml` 已覆盖。
- 团购到期回收 worker 在验收中禁用（SKY_SCHEDULING_ENABLED=false），到期路径经手动
  `PUT /admin/groupon/closeExpired` 验证；worker 自动触发未在长时间运行下观测。
- 隔离验收库保留于本机 MySQL 供排查：`cfd_acceptance_20261025_171958_964e06`、
  `cfd_acceptance_20261025_172323_5f0d56`、`cfd_acceptance_20261025_172814_885be4`。
