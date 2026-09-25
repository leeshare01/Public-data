package com.eshop.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eshop.ai.entity.Conversation;
import com.eshop.ai.entity.ConversationMessage;
import com.eshop.ai.mapper.ConversationMapper;
import com.eshop.ai.mapper.ConversationMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对话管理服务
 */
@Service
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;

    public ConversationService(ConversationMapper conversationMapper,
                               ConversationMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    // ========== 对话 CRUD ==========

    /** 创建新对话 */
    @Transactional
    public Conversation createConversation(Long userId) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle("新对话");
        conv.setMessageCount(0);
        conv.setStatus(1);
        conversationMapper.insert(conv);
        return conv;
    }

    /** 获取用户的对话列表（分页） */
    public Page<Conversation> getUserConversations(Long userId, int page, int size) {
        LambdaQueryWrapper<Conversation> qw = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdateTime);
        return conversationMapper.selectPage(new Page<>(page, size), qw);
    }

    /** 获取对话 */
    public Conversation getById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /** 删除对话（连带删除消息） */
    @Transactional
    public void deleteConversation(Long conversationId) {
        messageMapper.delete(new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }

    /** 更新对话标题 */
    public void updateTitle(Long conversationId, String title) {
        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setTitle(title);
        conversationMapper.updateById(conv);
    }

    // ========== 消息 CRUD ==========

    /** 保存一条消息 */
    @Transactional
    public void saveMessage(ConversationMessage msg) {
        messageMapper.insert(msg);
        // 更新对话的消息计数（message_count +1）
        Conversation conv = conversationMapper.selectById(msg.getConversationId());
        if (conv != null) {
            Conversation update = new Conversation();
            update.setId(conv.getId());
            update.setMessageCount(conv.getMessageCount() != null ? conv.getMessageCount() + 1 : 1);
            conversationMapper.updateById(update);
        }
    }

    /** 获取对话的消息列表（分页，按时间升序） */
    public Page<ConversationMessage> getMessages(Long conversationId, int page, int size) {
        LambdaQueryWrapper<ConversationMessage> qw = new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId)
                .orderByAsc(ConversationMessage::getCreateTime);
        return messageMapper.selectPage(new Page<>(page, size), qw);
    }

    /** 获取对话的完整消息历史（用于构建 DeepSeek 上下文） */
    public List<ConversationMessage> getMessageHistory(Long conversationId) {
        LambdaQueryWrapper<ConversationMessage> qw = new LambdaQueryWrapper<ConversationMessage>()
                .eq(ConversationMessage::getConversationId, conversationId)
                .orderByAsc(ConversationMessage::getCreateTime);
        return messageMapper.selectList(qw);
    }
}
