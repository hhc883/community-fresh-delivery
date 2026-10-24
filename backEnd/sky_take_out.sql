/*
 Navicat Premium Data Transfer

 Source Server         : TS
 Source Server Type    : MySQL
 Source Server Version : 80012
 Source Host           : localhost:30306
 Source Schema         : sky_take_out

 Target Server Type    : MySQL
 Target Server Version : 80012
 File Encoding         : 65001

 Date: 26/04/2024 20:09:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建数据库并切换（原文件缺失，直接导入会报 Unknown database 'sky_take_out'）
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `sky_take_out` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `sky_take_out`;

-- ----------------------------
-- Table structure for address_book
-- ----------------------------
DROP TABLE IF EXISTS `address_book`;
CREATE TABLE `address_book`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `consignee` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '默认 0 否 1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '地址簿' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int(11) NULL DEFAULT NULL COMMENT '类型   1 菜品分类 2 套餐分类',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '分类名称',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '顺序',
  `status` int(11) NULL DEFAULT NULL COMMENT '分类状态 0:禁用，1:启用',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `update_user` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_category_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '菜品及套餐分类' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dish
-- ----------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '菜品名称',
  `category_id` bigint(20) NOT NULL COMMENT '菜品分类id',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '菜品价格',
  `image` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '图片',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '描述信息',
  `status` int(11) NULL DEFAULT 1 COMMENT '0 停售 1 起售',
  `is_groupon` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否参与拼团 0否 1是',
  `groupon_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '拼团价（成团价）',
  `groupon_size` int(11) NULL DEFAULT 2 COMMENT '成团所需人数',
  `groupon_valid_hours` int(11) NULL DEFAULT 24 COMMENT '拼团有效时长（小时）',
  `stock` int(11) NULL DEFAULT 0 COMMENT '库存',
  `unit` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '规格单位，如 500g/份/箱',
  `origin` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '产地',
  `is_fresh` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否生鲜 0否 1是',
  `sold_count` int(11) NULL DEFAULT 0 COMMENT '销量',
  `promo_tag` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '促销标签',
  `seckill` tinyint(1) NULL DEFAULT 0 COMMENT '是否秒杀：0否 1是',
  `seckill_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '秒杀价',
  `tags` varchar(128) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '特性标签',
  `limit_buy` int(11) NULL DEFAULT NULL COMMENT '限购数量',
  `good_rate` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT '99%' COMMENT '好评率',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `update_user` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_dish_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '菜品' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for dish_flavor
-- ----------------------------
DROP TABLE IF EXISTS `dish_flavor`;
CREATE TABLE `dish_flavor`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint(20) NOT NULL COMMENT '菜品',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '口味名称',
  `value` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '口味数据list',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 120 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '菜品口味关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '密码',
  `phone` varchar(11) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '身份证号',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `update_user` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '员工信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for order_detail
-- ----------------------------
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '名字',
  `image` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '图片',
  `order_id` bigint(20) NOT NULL COMMENT '订单id',
  `dish_id` bigint(20) NULL DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint(20) NULL DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '口味',
  `number` int(11) NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` decimal(10, 2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '订单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '订单号',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款',
  `user_id` bigint(20) NOT NULL COMMENT '下单用户',
  `address_book_id` bigint(20) NULL DEFAULT NULL COMMENT '地址id（自提订单为 NULL）',
  `order_time` datetime(0) NOT NULL COMMENT '下单时间',
  `checkout_time` datetime(0) NULL DEFAULT NULL COMMENT '结账时间',
  `pay_method` int(11) NOT NULL DEFAULT 1 COMMENT '支付方式 1微信,2支付宝',
  `pay_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount` decimal(10, 2) NOT NULL COMMENT '实收金额',
  `remark` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '备注',
  `phone` varchar(11) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '地址',
  `user_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '用户名称',
  `consignee` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` datetime(0) NULL DEFAULT NULL COMMENT '订单取消时间',
  `estimated_delivery_time` datetime(0) NULL DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '配送状态  1立即送出  0选择具体时间',
  `delivery_time` datetime(0) NULL DEFAULT NULL COMMENT '送达时间',
  `pack_amount` int(11) NULL DEFAULT NULL COMMENT '打包费',
  `tableware_number` int(11) NULL DEFAULT NULL COMMENT '餐具数量',
  `tableware_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '餐具数量状态  1按餐量提供  0选择具体数量',
  `pickup_point_id` bigint(20) NULL DEFAULT NULL COMMENT '社区团购自提点id（团购订单使用）',
  `pickup_point_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '自提点名称',
  `pickup_time_slot` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '预约自提时段',
  `pickup_person_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '提货人姓名',
  `pickup_person_phone` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '提货人电话',
  `pickup_code` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '8位提货核销码',
  `verify_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '核销状态 0待核销 1已核销',
  `verify_time` datetime(0) NULL DEFAULT NULL COMMENT '核销时间',
  `is_groupon` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否拼团订单 0否 1是',
  `groupon_id` bigint(20) NULL DEFAULT NULL COMMENT '关联拼团活动id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_orders_pickup_code`(`pickup_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for setmeal
-- ----------------------------
DROP TABLE IF EXISTS `setmeal`;
CREATE TABLE `setmeal`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint(20) NOT NULL COMMENT '菜品分类id',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '套餐名称',
  `price` decimal(10, 2) NOT NULL COMMENT '套餐价格',
  `status` int(11) NULL DEFAULT 1 COMMENT '售卖状态 0:停售 1:起售',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '图片',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `update_user` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_setmeal_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '套餐' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for setmeal_dish
-- ----------------------------
DROP TABLE IF EXISTS `setmeal_dish`;
CREATE TABLE `setmeal_dish`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` bigint(20) NULL DEFAULT NULL COMMENT '套餐id',
  `dish_id` bigint(20) NULL DEFAULT NULL COMMENT '菜品id',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '菜品名称 （冗余字段）',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '菜品单价（冗余字段）',
  `copies` int(11) NULL DEFAULT NULL COMMENT '菜品份数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '套餐菜品关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for shopping_cart
-- ----------------------------
DROP TABLE IF EXISTS `shopping_cart`;
CREATE TABLE `shopping_cart`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '商品名称',
  `image` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '图片',
  `user_id` bigint(20) NOT NULL COMMENT '主键',
  `dish_id` bigint(20) NULL DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint(20) NULL DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '口味',
  `number` int(11) NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` decimal(10, 2) NOT NULL COMMENT '金额',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '购物车' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '头像',
  `password` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '密码',
  `balance` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '余额',
  `create_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '用户信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 社区团购扩展：商品增加生鲜属性与拼团字段
--
-- 【全新导入】无需执行本段：dish 的建表语句已包含以下字段。
-- 【旧库升级】dish 表已存在时，必须手动执行本段 ALTER，否则后端查询
--   is_groupon 会报 Unknown column 'is_groupon'。
--   若字段已存在，MySQL 会报 duplicate column，忽略即可。
-- ----------------------------
-- ALTER TABLE `dish`
--   ADD COLUMN `is_groupon` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否参与拼团 0否 1是',
--   ADD COLUMN `groupon_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '拼团价（成团价）',
--   ADD COLUMN `groupon_size` int(11) NULL DEFAULT 2 COMMENT '成团所需人数',
--   ADD COLUMN `groupon_valid_hours` int(11) NULL DEFAULT 24 COMMENT '拼团有效时长（小时）',
--   ADD COLUMN `stock` int(11) NULL DEFAULT 0 COMMENT '库存',
--   ADD COLUMN `unit` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '规格单位，如 500g/份/箱',
--   ADD COLUMN `origin` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '产地',
--   ADD COLUMN `is_fresh` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否生鲜 0否 1是';
--
--   -- 旧库的 orders 表同样需要补充团购字段：
-- ALTER TABLE `orders`
--   ADD COLUMN `pickup_point_id` bigint(20) NULL DEFAULT NULL COMMENT '社区团购自提点id（团购订单使用）',
--   ADD COLUMN `is_groupon` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否拼团订单 0否 1是',
--   ADD COLUMN `groupon_id` bigint(20) NULL DEFAULT NULL COMMENT '关联拼团活动id';

-- ----------------------------
-- 自提点（社区团购以社区自提点为核心履约方式）
-- ----------------------------
DROP TABLE IF EXISTS `pickup_point`;
CREATE TABLE `pickup_point`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '自提点名称',
  `community` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '所属社区',
  `address` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '详细地址',
  `contact` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '联系人',
  `contact_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '联系人姓名',
  `phone` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '联系电话',
  `contact_phone` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '联系电话',
  `distance` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT '260m' COMMENT '距离展示',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度',
  `image` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '门面实拍图',
  `service_tags` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT '支持冷冻/冷藏' COMMENT '服务标签',
  `open_time` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '营业时间',
  `status` int(11) NULL DEFAULT 1 COMMENT '0 停用 1 启用',
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '社区自提点' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 拼团活动（一次开团 = 一条记录，团长 + 待加入的团员）
-- ----------------------------
DROP TABLE IF EXISTS `groupon`;
CREATE TABLE `groupon`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint(20) NOT NULL COMMENT '拼团商品id',
  `leader_user_id` bigint(20) NOT NULL COMMENT '团长用户id',
  `groupon_price` decimal(10, 2) NOT NULL COMMENT '拼团价快照',
  `required_size` int(11) NOT NULL COMMENT '成团所需人数',
  `current_size` int(11) NOT NULL DEFAULT 1 COMMENT '当前已参团人数',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '1拼团中 2已成团 3已失败 4已过期',
  `expire_time` datetime(0) NOT NULL COMMENT '拼团截止时间',
  `complete_time` datetime(0) NULL DEFAULT NULL COMMENT '成团时间',
  `pickup_point_id` bigint(20) NULL DEFAULT NULL COMMENT '自提点id',
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_groupon_dish`(`dish_id`) USING BTREE,
  INDEX `idx_groupon_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '拼团活动' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 拼团成员（记录每个参团的人及其订单）
-- ----------------------------
DROP TABLE IF EXISTS `groupon_member`;
CREATE TABLE `groupon_member`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `groupon_id` bigint(20) NOT NULL COMMENT '拼团活动id',
  `user_id` bigint(20) NOT NULL COMMENT '参团用户id',
  `order_id` bigint(20) NULL DEFAULT NULL COMMENT '关联订单id',
  `is_leader` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否团长 0否 1是',
  `number` int(11) NOT NULL DEFAULT 1 COMMENT '购买数量',
  `amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额',
  `create_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_groupon`(`groupon_id`) USING BTREE,
  UNIQUE INDEX `idx_groupon_user`(`groupon_id`, `user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_bin COMMENT = '拼团成员' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 自提点示例数据（方便开箱即用，可自行修改/删除）
-- ----------------------------
INSERT INTO `pickup_point` (`name`, `community`, `address`, `contact`, `contact_name`, `phone`, `contact_phone`, `distance`, `latitude`, `longitude`, `image`, `service_tags`, `open_time`, `status`, `create_time`, `update_time`) VALUES
('高黎柱发士多店(请自备袋子谢谢)', '容桂高黎', '广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号', '柱哥', '柱哥', '13800138000', '13800138000', '260m', 22.7725000, 113.3122000, '/static/images/png/dish-1.png', '支持冷冻/冷藏', '每日 07:00-22:00', 1, now(), now()),
('阳光小区自提点', '阳光小区', '1号楼架空层快递驿站旁', '张师傅', '张师傅', '13800000001', '13800000001', '350m', 39.9087000, 116.3975000, '/static/images/png/dish-1.png', '支持冷冻/冷藏', '每日 07:00-21:00', 1, now(), now()),
('和谐家园自提点', '和谐家园', '南门物业服务中心', '李阿姨', '李阿姨', '13800000002', '13800000002', '700m', 39.9182000, 116.4053000, '/static/images/png/dish-1.png', '支持冷冻/冷藏', '每日 08:00-20:00', 1, now(), now()),
('幸福里自提点', '幸福里', '3期地下车库入口旁', '王师傅', '王师傅', '13800000003', '13800000003', '1050m', 39.8994000, 116.4121000, '/static/images/png/dish-1.png', '支持冷冻/冷藏', '每日 07:30-21:30', 1, now(), now());

-- ----------------------------
-- 生鲜拼团商品示例（挂在分类1下；请按实际情况调整 category_id）
-- 用 INSERT IGNORE + dish.name 的唯一索引 idx_dish_name 保证重复执行不报错
-- ----------------------------
INSERT IGNORE INTO `dish` (`name`, `category_id`, `price`, `image`, `description`, `status`,
                           `is_groupon`, `groupon_price`, `groupon_size`, `groupon_valid_hours`,
                           `stock`, `unit`, `origin`, `is_fresh`, `create_time`, `update_time`)
VALUES ('云南高原红提', 1, 29.90, '', '当季新鲜红提，甜度高', 1, 1, 19.90, 3, 24, 200, '1000g/串', '云南', 1, now(), now()),
       ('山东烟台苹果', 1, 19.90, '', '脆甜多汁，现摘现发', 1, 1, 12.90, 5, 48, 500, '2500g/箱', '山东烟台', 1, now(), now()),
       ('本地散养土鸡蛋', 1, 25.00, '', '当天新鲜直供', 1, 1, 16.90, 4, 24, 300, '30枚/盒', '本地', 1, now(), now());

SET FOREIGN_KEY_CHECKS = 1;
