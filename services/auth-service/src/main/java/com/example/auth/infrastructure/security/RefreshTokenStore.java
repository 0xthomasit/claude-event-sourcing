package com.example.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Manages opaque refresh tokens in Redis.
 *
 * Key schema:
 *   refresh:{token}  → userId   (TTL = refreshTokenExpiry)
 *
 * On rotation: delete old token, store new token.
 * On logout:   delete refresh token + blacklist access token.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String REFRESH_PREFIX   = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties       jwtProperties;

    public String issue(String userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + token,
                userId,
                Duration.ofSeconds(jwtProperties.refreshTokenExpirySeconds()));
        return token;
    }

    public String validate(String token) {
        // Returns userId if valid, null if expired/not found
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + token);
    }

    public void rotate(String oldToken, String newToken, String userId) {
        redisTemplate.delete(REFRESH_PREFIX + oldToken);
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + newToken,
                userId,
                Duration.ofSeconds(jwtProperties.refreshTokenExpirySeconds()));
    }

    public void revoke(String refreshToken) {
        redisTemplate.delete(REFRESH_PREFIX + refreshToken);
    }

    public void blacklistAccessToken(String accessToken, long remainingSeconds) {
        if (remainingSeconds > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "revoked",
                    Duration.ofSeconds(remainingSeconds));
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
    }
}