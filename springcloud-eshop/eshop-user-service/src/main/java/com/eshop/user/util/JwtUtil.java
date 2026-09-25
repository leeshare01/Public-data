package com.eshop.user.util;

import com.eshop.common.constant.AuthConstant;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（签名密钥需与 Gateway 一致）
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey = Keys.hmacShaKeyFor(
            "Eshop_JWT_Secret_Key_2026_SpringCloud_Alibaba_DeepSeek".getBytes(StandardCharsets.UTF_8)
    );

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + AuthConstant.TOKEN_EXPIRE_SECONDS * 1000))
                .signWith(secretKey)
                .compact();
    }
}
