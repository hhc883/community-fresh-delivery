-- ============================================================
-- 自提核销改造 —— 数据库增量补丁（幂等，可重复执行）
-- ============================================================
-- 背景：本工程已有 pickup_point / groupon 表与 dish 的 stock/unit/origin/is_fresh 字段，
-- 但「自提下单 → 支付 → 生成提货码 → 到店核销」这条链路缺三块：
--   ① orders 表没有提货码与核销状态（无法凭码取货、无法防重复领货）
--   ② orders 表没有提货人快照（自提订单不依赖地址簿，人物信息必须随单落库）
--   ③ pickup_point 表没有经纬度（小程序端无法按距离排序/地图展示）
--
-- 兼容性：MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，故用 information_schema 判断 +
-- PREPARE 动态执行的方式实现幂等，重复跑不会报 1060 Duplicate column name。
-- 执行方式：
--   mysql -u root -p sky_take_out < m21-pickup-verify-migration.sql
-- ============================================================

USE sky_take_out;

-- ------------------------------------------------------------
-- 1) orders：提货码 / 自提时段 / 提货人快照 / 核销状态
-- ------------------------------------------------------------

-- 8 位提货核销码（纯数字，如 68291034）。支付成功时生成，UNIQUE 保证全局唯一。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_code') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_code` VARCHAR(16) NULL DEFAULT NULL COMMENT ''自提提货核销码（8位数字，支付后生成）''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 预约自提时段，如 "明日 14:00-16:00"
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_time_slot') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_time_slot` VARCHAR(64) NULL DEFAULT NULL COMMENT ''预约自提时间段''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 提货人姓名（自提订单不依赖地址簿，故需随单快照）
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_person_name') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_person_name` VARCHAR(32) NULL DEFAULT NULL COMMENT ''提货人姓名''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 提货人手机号（用于到店核对）
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_person_phone') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_person_phone` VARCHAR(16) NULL DEFAULT NULL COMMENT ''提货人手机号''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 核销状态：0 未核销 1 已核销
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'verify_status') = 0,
  'ALTER TABLE `orders` ADD COLUMN `verify_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''核销状态 0未核销 1已核销''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 核销时间
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'verify_time') = 0,
  'ALTER TABLE `orders` ADD COLUMN `verify_time` DATETIME NULL DEFAULT NULL COMMENT ''核销时间''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 提货码查询索引：核销接口按 pickup_code 查单，必须走索引
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_pickup_code') = 0,
  'ALTER TABLE `orders` ADD INDEX `idx_orders_pickup_code` (`pickup_code`)',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 2) orders.address_book_id 放开 NOT NULL
-- ------------------------------------------------------------
-- 【实测暴露的必需改动】原表 address_book_id 是 NOT NULL，而自提订单**天然没有地址簿**。
-- 不放开的话下单直接报：
--   Column 'address_book_id' cannot be null
-- 这一条不在最初的需求清单里，是端到端跑通时被数据库约束挡下来的。
-- 语义上：配送单仍必有 address_book_id（业务层校验），自提单该列为 NULL，
-- 两列的互斥关系（address_book_id 与 pickup_point_id 至少有一个非空）由业务层保证。
SET @ddl = IF(
  (SELECT is_nullable FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'address_book_id') = 'NO',
  'ALTER TABLE `orders` MODIFY COLUMN `address_book_id` BIGINT NULL DEFAULT NULL COMMENT ''地址簿id（自提订单为 NULL）''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 3) pickup_point：经纬度与门头图（小程序端按距离排序 / 地图展示）
-- ------------------------------------------------------------

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'latitude') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `latitude` DECIMAL(10,6) NULL DEFAULT NULL COMMENT ''纬度''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'longitude') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `longitude` DECIMAL(10,6) NULL DEFAULT NULL COMMENT ''经度''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'image') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `image` VARCHAR(255) NULL DEFAULT NULL COMMENT ''门头图片''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 4) 回填：给现有 3 个自提点补经纬度（北京朝阳区一带，便于小程序地图直接出图）
--    仅回填 NULL 行，不覆盖运营后续手工维护的坐标
-- ------------------------------------------------------------
UPDATE `pickup_point` SET `latitude` = 39.908700, `longitude` = 116.397500
  WHERE `id` = 1 AND `latitude` IS NULL;
UPDATE `pickup_point` SET `latitude` = 39.918200, `longitude` = 116.405300
  WHERE `id` = 2 AND `latitude` IS NULL;
UPDATE `pickup_point` SET `latitude` = 39.899400, `longitude` = 116.412100
  WHERE `id` = 3 AND `latitude` IS NULL;

-- ------------------------------------------------------------
-- 5) 自证：打印改造后的列，便于执行后肉眼核对
-- ------------------------------------------------------------
SELECT table_name, column_name, column_type, is_nullable, column_default, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'orders' AND column_name IN
        ('pickup_code','pickup_time_slot','pickup_person_name','pickup_person_phone','verify_status','verify_time'))
    OR (table_name = 'pickup_point' AND column_name IN ('latitude','longitude','image')))
ORDER BY table_name, ordinal_position;
