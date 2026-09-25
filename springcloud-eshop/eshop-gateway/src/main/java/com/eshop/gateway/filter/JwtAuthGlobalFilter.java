package com.eshop.gateway.filter;

import com.eshop.common.constant.AuthConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 全局鉴权过滤器
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecretKey secretKey = Keys.hmacShaKeyFor(
            "Eshop_JWT_Secret_Key_2026_SpringCloud_Alibaba_DeepSeek".getBytes(StandardCharsets.UTF_8)
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单路径放行（但 /admin/ 子路径不由白名单跳过，必须走 JWT 鉴权）
        // 注意：对 /api/product/* 只放行 GET 请求，PUT/POST/DELETE 需 JWT 鉴权
        boolean isAdminPath = path.startsWith("/api/") && path.contains("/admin/");
        if (!isAdminPath) {
            boolean inWhiteList = false;
            for (String pattern : AuthConstant.WHITE_LIST) {
                if (pathMatcher.match(pattern, path)) {
                    inWhiteList = true;
                    break;
                }
            }
            // 对商品相关写操作（PUT/POST/DELETE）不予以白名单放行
            if (inWhiteList && path.startsWith("/api/product/")) {
                String method = exchange.getRequest().getMethod().name();
                if (!"GET".equalsIgnoreCase(method) && !"OPTIONS".equalsIgnoreCase(method)) {
                    inWhiteList = false;
                }
            }
            if (inWhiteList) {
                return chain.filter(exchange);
            }
        }

        // 提取 Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(AuthConstant.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(AuthConstant.BEARER)) {
            log.warn("缺少Token: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(AuthConstant.BEARER.length());
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = claimsJws.getPayload();

            // 将用户信息转发给下游服务
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r
                            .header(AuthConstant.USER_ID_HEADER, userId)
                            .header(AuthConstant.USERNAME_HEADER, username)
                            .header(AuthConstant.ROLE_HEADER, role)
                    )
                    .build();
            return chain.filter(mutatedExchange);

        } catch (JwtException e) {
            log.warn("Token无效: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
