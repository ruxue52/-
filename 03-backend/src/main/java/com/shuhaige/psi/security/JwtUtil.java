package com.shuhaige.psi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发 / 解析，载荷含 userId、username、userType，24h 有效期
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours:24}")
    private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发员工 token */
    public String generate(Long userId, String username) {
        return generate(userId, username, LoginUser.TYPE_STAFF);
    }

    /** 签发指定身份的 token（员工 STAFF / 会员 CUSTOMER） */
    public String generate(Long userId, String username, String userType) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("userType", userType)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 token，失败返回 null */
    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Number uid = claims.get("userId", Number.class);
            String username = claims.get("username", String.class);
            String userType = claims.get("userType", String.class);
            return new LoginUser(uid == null ? null : uid.longValue(), username,
                    userType == null ? LoginUser.TYPE_STAFF : userType);
        } catch (Exception e) {
            log.debug("token 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
