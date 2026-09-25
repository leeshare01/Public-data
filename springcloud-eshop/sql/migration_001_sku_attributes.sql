-- ============================================================
-- Migration 001: 商品 SKU + 属性 表结构
-- ============================================================

USE eshop_product;

-- 1. 给 product 表补充字段
-- 注意：如果已经存在会报错，忽略即可
ALTER TABLE `product`
    ADD COLUMN `sub_images` JSON DEFAULT NULL COMMENT '轮播图URL数组' AFTER `main_image`,
    ADD COLUMN `keywords` VARCHAR(500) DEFAULT NULL COMMENT '搜索关键词，逗号分隔' AFTER `status`;

-- 2. SKU(库存)表
CREATE TABLE IF NOT EXISTS `product_sku` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    `product_id`     BIGINT         NOT NULL COMMENT '商品ID',
    `sku_code`       VARCHAR(50)    DEFAULT NULL COMMENT 'SKU编码',
    `spec_values`    JSON           DEFAULT NULL COMMENT '规格值，如 {"颜色":"红色","尺寸":"M"}',
    `price`          DECIMAL(10,2)  NOT NULL COMMENT 'SKU售价',
    `stock`          INT            NOT NULL DEFAULT 0 COMMENT '库存数量',
    `locked_stock`   INT            DEFAULT 0 COMMENT '锁定库存',
    `image`          VARCHAR(500)   DEFAULT NULL COMMENT 'SKU专属图片',
    `sort_order`     INT            DEFAULT 0 COMMENT '排序',
    `create_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sku_product_id` (`product_id`),
    KEY `idx_sku_code` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 3. 商品参数表
CREATE TABLE IF NOT EXISTS `product_attribute` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '参数ID',
    `product_id`  BIGINT       NOT NULL COMMENT '商品ID',
    `attr_name`   VARCHAR(100) NOT NULL COMMENT '参数名称',
    `attr_value`  VARCHAR(500) NOT NULL COMMENT '参数值',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_attr_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品参数表';

-- 4. 先插入测试分类
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`) VALUES
-- 一级分类
(1,  '服装',    0, 1, NULL, 1),
(2,  '数码',    0, 1, NULL, 2),
(3,  '家居',    0, 1, NULL, 3),
(4,  '食品',    0, 1, NULL, 4),
(5,  '美妆',    0, 1, NULL, 5),
(6,  '运动',    0, 1, NULL, 6),
-- 服装子分类
(11, '男士T恤',  1, 2, NULL, 1),
(12, '男士衬衫', 1, 2, NULL, 2),
(13, '男士外套', 1, 2, NULL, 3),
(14, '男士裤子', 1, 2, NULL, 4),
(15, '女士连衣裙',1, 2, NULL, 5),
(16, '女士上衣', 1, 2, NULL, 6),
(17, '女士外套', 1, 2, NULL, 7),
(18, '女士半身裙',1, 2, NULL, 8),
-- 数码子分类
(21, '手机配件', 2, 2, NULL, 1),
(22, '耳机音箱', 2, 2, NULL, 2),
(23, '电脑外设', 2, 2, NULL, 3),
(24, '充电设备', 2, 2, NULL, 4),
(25, '智能穿戴', 2, 2, NULL, 5),
-- 家居子分类
(31, '厨房用品', 3, 2, NULL, 1),
(32, '卧室用品', 3, 2, NULL, 2),
(33, '客厅用品', 3, 2, NULL, 3),
(34, '卫浴用品', 3, 2, NULL, 4),
(35, '收纳整理', 3, 2, NULL, 5),
-- 食品子分类
(41, '休闲零食', 4, 2, NULL, 1),
(42, '冲饮酒水', 4, 2, NULL, 2),
(43, '生鲜食品', 4, 2, NULL, 3),
-- 美妆子分类
(51, '面部护理', 5, 2, NULL, 1),
(52, '身体护理', 5, 2, NULL, 2),
(53, '美妆工具', 5, 2, NULL, 3),
-- 运动子分类
(61, '运动鞋',   6, 2, NULL, 1),
(62, '运动服饰', 6, 2, NULL, 2),
(63, '运动器材', 6, 2, NULL, 3);
