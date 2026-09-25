-- ============================================================
-- 微服务电商智能导购平台 — 数据库初始化脚本
-- ============================================================

-- 用户数据库
CREATE DATABASE IF NOT EXISTS eshop_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eshop_user;

CREATE TABLE IF NOT EXISTS `user` (
    `id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`  VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`  VARCHAR(255) NOT NULL COMMENT '密码(bcrypt)',
    `nickname`  VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `phone`     VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`     VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar`    VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `gender`    TINYINT      DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    `role`      VARCHAR(20)  DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
    `status`    TINYINT      DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    `create_time` DATETIME   DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认用户（密码均为 bcrypt 加密）
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `email`, `role`, `status`) VALUES
('admin',    '$2b$10$WqIIPT7b1xKj6jiO1vGXqe1xsJw4eQta0Wq7Lf7IvtAzTP5eFqNka', '管理员',   '13800000001', 'admin@eshop.com', 'ADMIN', 1),
('test',     '$2b$10$aBBzWDtwmU9E4GD05yVdIeBaygwqu9iEkzv4ELPZPJTnackAST7w2', '测试用户', '13800000002', 'test@eshop.com',  'USER',  1),
('testuser', '$2b$10$z4mg5seDozSTglLL5O.sm.ofQmDITqmERK.bT/fgAw9fFXS11T1ou', 'TestUser', '13800000003', 'testuser@eshop.com', 'USER', 1);

-- 购物车数据库
CREATE DATABASE IF NOT EXISTS eshop_cart DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eshop_cart;

CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
    `product_id`  BIGINT        NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
    `price`       DECIMAL(10,2) NOT NULL COMMENT '单价',
    `quantity`    INT           NOT NULL DEFAULT 1 COMMENT '数量',
    `selected`    TINYINT(1)    DEFAULT 1 COMMENT '是否选中',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `address` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `receiver_name`  VARCHAR(50)  NOT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20)  NOT NULL COMMENT '收货电话',
    `province`       VARCHAR(50)  NOT NULL COMMENT '省',
    `city`           VARCHAR(50)  NOT NULL COMMENT '市',
    `district`       VARCHAR(50)  NOT NULL COMMENT '区',
    `detail_address` VARCHAR(200) NOT NULL COMMENT '详细地址',
    `is_default`     TINYINT(1)   DEFAULT 0 COMMENT '是否默认',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 商品数据库
CREATE DATABASE IF NOT EXISTS eshop_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eshop_product;

CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父分类ID',
    `level`       TINYINT      DEFAULT 1 COMMENT '层级',
    `icon`        VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS `product` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(200)  NOT NULL COMMENT '商品名称',
    `subtitle`       VARCHAR(500)  DEFAULT NULL COMMENT '副标题',
    `brand`          VARCHAR(100)  DEFAULT NULL COMMENT '品牌',
    `description`    TEXT          DEFAULT NULL COMMENT '商品详情HTML',
    `main_image`     VARCHAR(500)  DEFAULT NULL COMMENT '主图URL',
    `category_id`    BIGINT        NOT NULL COMMENT '分类ID',
    `price`          DECIMAL(10,2) NOT NULL COMMENT '价格',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `sales`          INT           DEFAULT 0 COMMENT '销量',
    `status`         TINYINT       DEFAULT 1 COMMENT '状态 0-下架 1-上架',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单数据库
CREATE DATABASE IF NOT EXISTS eshop_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eshop_order;

CREATE TABLE IF NOT EXISTS `orders` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`          VARCHAR(32)   NOT NULL COMMENT '订单号',
    `user_id`           BIGINT        NOT NULL COMMENT '用户ID',
    `total_amount`      DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `pay_amount`        DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `status`            TINYINT       DEFAULT 0 COMMENT '状态 0-待付款 1-已付款 2-已发货 3-已完成 4-已取消',
    `consignee_name`    VARCHAR(50)   DEFAULT NULL COMMENT '收货人',
    `consignee_phone`   VARCHAR(20)   DEFAULT NULL COMMENT '收货电话',
    `consignee_address` VARCHAR(500)  DEFAULT NULL COMMENT '收货地址',
    `remark`            VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`      VARCHAR(32)   NOT NULL COMMENT '订单号',
    `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
    `product_name`  VARCHAR(200)  NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(500)  DEFAULT NULL COMMENT '商品图片',
    `price`         DECIMAL(10,2) NOT NULL COMMENT '单价',
    `quantity`      INT           NOT NULL COMMENT '数量',
    `subtotal`      DECIMAL(10,2) NOT NULL COMMENT '小计',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- AI 导购数据库
CREATE DATABASE IF NOT EXISTS eshop_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eshop_ai;

CREATE TABLE IF NOT EXISTS `conversation` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `title`         VARCHAR(200) DEFAULT NULL COMMENT '对话标题',
    `message_count` INT          DEFAULT 0 COMMENT '消息数',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话表';
