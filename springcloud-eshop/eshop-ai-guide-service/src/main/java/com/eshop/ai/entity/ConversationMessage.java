package com.eshop.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 对话消息
 */
@TableName("conversation_message")
public class ConversationMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    private String contentType;

    private String relatedProductIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getRelatedProductIds() { return relatedProductIds; }
    public void setRelatedProductIds(String relatedProductIds) { this.relatedProductIds = relatedProductIds; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
