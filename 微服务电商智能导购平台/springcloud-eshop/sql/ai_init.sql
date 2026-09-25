-- ============================================================
-- AI 导购服务 — 补充表（conversation 表已在 init.sql 中创建）
-- ============================================================

USE eshop_ai;

-- 对话消息表（存储每条对话消息）
CREATE TABLE IF NOT EXISTS `conversation_message` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `conversation_id`   BIGINT        NOT NULL COMMENT '对话ID',
    `role`              VARCHAR(20)   NOT NULL COMMENT '角色 user/assistant/system',
    `content`           TEXT          NOT NULL COMMENT '消息内容',
    `content_type`      VARCHAR(20)   DEFAULT 'text' COMMENT '内容类型 text/product_recommend',
    `related_product_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联商品ID列表，逗号分隔',
    `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- 建议初始测试数据：插入一些商品分类知识（可选，后续 RAG 会替代）
INSERT IGNORE INTO `conversation` (`id`, `user_id`, `title`, `message_count`, `status`)
VALUES (0, 0, '系统初始化', 0, 0);
