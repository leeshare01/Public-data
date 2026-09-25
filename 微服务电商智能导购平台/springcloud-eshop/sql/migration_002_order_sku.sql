-- ============================================================
-- Migration 002: order_item 表增加 sku_id 和 spec_values 字段
-- ============================================================

USE eshop_order;

ALTER TABLE `order_item`
    ADD COLUMN `sku_id`      BIGINT       DEFAULT NULL COMMENT 'SKU ID' AFTER `product_id`,
    ADD COLUMN `spec_values` VARCHAR(200) DEFAULT NULL COMMENT '规格文字，如 "红色 / M"' AFTER `product_image`;
