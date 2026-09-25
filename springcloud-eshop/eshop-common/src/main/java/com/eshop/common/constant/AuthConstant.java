package com.eshop.common.constant;

/**
 * 认证相关常量
 */
public interface AuthConstant {

    /** Token 请求头 */
    String AUTHORIZATION = "Authorization";
    /** Bearer 前缀 */
    String BEARER = "Bearer ";
    /** Token 前缀（Redis key） */
    String TOKEN_PREFIX = "token:";
    /** Token 过期时间（秒）- 24h */
    long TOKEN_EXPIRE_SECONDS = 86400L;

    /** 用户ID 请求头（Gateway 传递给下游服务） */
    String USER_ID_HEADER = "X-User-Id";
    /** 用户名 请求头 */
    String USERNAME_HEADER = "X-Username";
    /** 用户角色 请求头 */
    String ROLE_HEADER = "X-Role";

    /** 角色 */
    String ROLE_USER = "USER";
    String ROLE_ADMIN = "ADMIN";

    /** 白名单路径（无需认证） */
    String[] WHITE_LIST = {
            "/api/user/register",
            "/api/user/login",
            "/api/product/page",        // 公开 - 商品列表
            "/api/product/category/**", // 公开 - 分类查询
            "/api/product/*",           // 公开 - 商品详情 GET /api/product/{id}
            "/api/ai/health",
            "/api/ai/chat/stream",      // SSE 流式的 OPTIONS 预检
    };
}
