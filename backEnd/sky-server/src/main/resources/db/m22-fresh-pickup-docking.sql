-- ============================================================
-- 生鲜自提前端与后端无缝对接 —— 数据库增量补丁（幂等，可重复执行）
-- ============================================================
-- Target database must be selected explicitly by the migration runner.

-- ------------------------------------------------------------
-- 1) 自提网点表 (pickup_point) 字段补充与对齐
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `pickup_point` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '网点名称（如：高黎柱发士多店）',
  `address` varchar(255) NOT NULL COMMENT '详细地址',
  `contact` varchar(32) DEFAULT NULL COMMENT '联系人',
  `contact_name` varchar(32) DEFAULT NULL COMMENT '联系人（如：柱哥）',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `distance` varchar(20) DEFAULT '260m' COMMENT '距离展示',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `image` varchar(255) DEFAULT NULL COMMENT '门面实拍图',
  `service_tags` varchar(64) DEFAULT '支持冷冻/冷藏' COMMENT '服务标签',
  `status` tinyint DEFAULT 1 COMMENT '1营业 0停业',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自提网点表';

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'contact_name') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `contact_name` VARCHAR(32) NULL DEFAULT NULL COMMENT ''联系人''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'contact_phone') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `contact_phone` VARCHAR(20) NULL DEFAULT NULL COMMENT ''联系电话''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'distance') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `distance` VARCHAR(20) NULL DEFAULT ''260m'' COMMENT ''距离展示''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'service_tags') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `service_tags` VARCHAR(64) NULL DEFAULT ''支持冷冻/冷藏'' COMMENT ''服务标签''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'latitude') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `latitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT ''纬度''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'longitude') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `longitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT ''经度''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pickup_point' AND column_name = 'image') = 0,
  'ALTER TABLE `pickup_point` ADD COLUMN `image` VARCHAR(255) NULL DEFAULT NULL COMMENT ''门头图片''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- Demo data is in backEnd/sql/002-demo-seed.sql; migrations never insert a store.

-- ------------------------------------------------------------
-- 2) 订单表 (orders) 自提字段对齐
-- ------------------------------------------------------------

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_point_id') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_point_id` BIGINT NULL DEFAULT NULL COMMENT ''自提点ID''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_point_name') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_point_name` VARCHAR(64) NULL DEFAULT NULL COMMENT ''自提点名称''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_time_slot') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_time_slot` VARCHAR(64) NULL DEFAULT NULL COMMENT ''自提时段''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_person_name') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_person_name` VARCHAR(32) NULL DEFAULT NULL COMMENT ''提货人姓名''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_person_phone') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_person_phone` VARCHAR(20) NULL DEFAULT NULL COMMENT ''提货人电话''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pickup_code') = 0,
  'ALTER TABLE `orders` ADD COLUMN `pickup_code` VARCHAR(16) NULL DEFAULT NULL COMMENT ''8位提货核销码''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'verify_status') = 0,
  'ALTER TABLE `orders` ADD COLUMN `verify_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''核销状态 0待核销 1已核销''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'verify_time') = 0,
  'ALTER TABLE `orders` ADD COLUMN `verify_time` DATETIME NULL DEFAULT NULL COMMENT ''核销时间''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND index_name = 'idx_orders_pickup_code') = 0,
  'ALTER TABLE `orders` ADD INDEX `idx_orders_pickup_code` (`pickup_code`)',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT is_nullable FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'address_book_id') = 'NO',
  'ALTER TABLE `orders` MODIFY COLUMN `address_book_id` BIGINT NULL DEFAULT NULL COMMENT ''地址簿id（自提订单为 NULL）''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 3) 商品表 (dish) 字段补充
-- ------------------------------------------------------------

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dish' AND column_name = 'sold_count') = 0,
  'ALTER TABLE `dish` ADD COLUMN `sold_count` INT NULL DEFAULT 0 COMMENT ''销量''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dish' AND column_name = 'promo_tag') = 0,
  'ALTER TABLE `dish` ADD COLUMN `promo_tag` VARCHAR(32) NULL DEFAULT NULL COMMENT ''促销标签（如：双旦特惠、直降¥100）''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dish' AND column_name = 'seckill') = 0,
  'ALTER TABLE `dish` ADD COLUMN `seckill` TINYINT NULL DEFAULT 0 COMMENT ''是否秒杀：0否 1是''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dish' AND column_name = 'seckill_price') = 0,
  'ALTER TABLE `dish` ADD COLUMN `seckill_price` DECIMAL(10,2) NULL DEFAULT NULL COMMENT ''秒杀价''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dish' AND column_name = 'tags') = 0,
  'ALTER TABLE `dish` ADD COLUMN `tags` VARCHAR(128) NULL DEFAULT NULL COMMENT ''特性标签逗号隔开''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dish' AND column_name = 'limit_buy') = 0,
  'ALTER TABLE `dish` ADD COLUMN `limit_buy` INT NULL DEFAULT NULL COMMENT ''限购数量''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dish' AND column_name = 'good_rate') = 0,
  'ALTER TABLE `dish` ADD COLUMN `good_rate` VARCHAR(16) NULL DEFAULT ''99%'' COMMENT ''好评率''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 4) 用户表 (user) 登录密码与余额补充
-- ------------------------------------------------------------

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'password') = 0,
  'ALTER TABLE `user` ADD COLUMN `password` VARCHAR(64) NULL DEFAULT NULL COMMENT ''登录密码（MD5）''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'balance') = 0,
  'ALTER TABLE `user` ADD COLUMN `balance` DECIMAL(10,2) NULL DEFAULT 0.00 COMMENT ''账户余额''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
