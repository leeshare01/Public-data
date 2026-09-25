package com.eshop.ai.controller;

import com.eshop.ai.service.AiGuideService;
import com.eshop.ai.service.ConversationService;
import com.eshop.common.dto.PageResult;
import com.eshop.common.result.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * AI 导购服务 — REST 接口
 */
@RestController
@RequestMapping("/api/ai")
public class AiGuideController {

    private final AiGuideService aiGuideService;
    private final ConversationService conversationService;

    public AiGuideController(AiGuideService aiGuideService,
                             ConversationService conversationService) {
        this.aiGuideService = aiGuideService;
        this.conversationService = conversationService;
    }

    // ========== 导购对话 ==========

    /**
     * 发送导购消息（非流式）
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> body) {
        Long conversationId = body.get("conversationId") instanceof Number
                ? ((Number) body.get("conversationId")).longValue() : 0;
        String message = (String) body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.error(400, "消息不能为空");
        }
        return Result.success(aiGuideService.chat(userId, conversationId, message));
    }

    /**
     * 发送导购消息（流式 SSE）
     */
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> body) {
        Long conversationId = body.get("conversationId") instanceof Number
                ? ((Number) body.get("conversationId")).longValue() : 0;
        String message = (String) body.get("message");
        if (message == null || message.trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"type\":\"error\",\"content\":\"消息不能为空\"}"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        // 匿名用户默认 userId=0
        if (userId == null) userId = 0L;
        return aiGuideService.chatStream(userId, conversationId, message);
    }

    // ========== 对话管理 ==========

    /**
     * 获取对话历史列表
     */
    @GetMapping("/conversations")
    public Result<PageResult<?>> getConversations(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageResult = conversationService.getUserConversations(userId, page, size);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()));
    }

    /**
     * 获取对话消息列表
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<PageResult<?>> getMessages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        // 验证对话属于当前用户
        var conv = conversationService.getById(conversationId);
        if (conv == null) return Result.notFound("对话不存在");
        if (!conv.getUserId().equals(userId)) return Result.forbidden();

        var pageResult = conversationService.getMessages(conversationId, page, size);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()));
    }

    /**
     * 删除对话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long conversationId) {
        var conv = conversationService.getById(conversationId);
        if (conv == null) return Result.notFound("对话不存在");
        if (!conv.getUserId().equals(userId)) return Result.forbidden();
        conversationService.deleteConversation(conversationId);
        return Result.success("删除成功", null);
    }

    /**
     * 生成对话标题
     */
    @PostMapping("/conversations/{conversationId}/title")
    public Result<Void> generateTitle(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long conversationId) {
        var conv = conversationService.getById(conversationId);
        if (conv == null) return Result.notFound("对话不存在");
        if (!conv.getUserId().equals(userId)) return Result.forbidden();
        // 取对话第一条消息作为标题
        var messages = conversationService.getMessageHistory(conversationId);
        if (!messages.isEmpty()) {
            String firstMsg = messages.get(0).getContent();
            String title = firstMsg.length() > 30 ? firstMsg.substring(0, 30) + "..." : firstMsg;
            conversationService.updateTitle(conversationId, title);
        }
        return Result.success("标题已生成", null);
    }

    // ========== 索引管理 ==========

    /**
     * 重新索引所有商品到向量库
     */
    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindex() {
        return Result.success(aiGuideService.reindex());
    }

    // ========== 健康检测 ==========

    /**
     * 健康检测接口
     * 返回各组件状态：DeepSeek、向量库、Embedding 等
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(aiGuideService.healthCheck());
    }
}
