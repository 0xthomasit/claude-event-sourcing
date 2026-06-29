package com.example.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirySeconds,    // 900  = 15 min
        long refreshTokenExpirySeconds    // 604800 = 7 days
) {}