package com.example.auth.infrastructure.security;

import com.example.auth.domain.model.Role;
import com.example.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User             testUser;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha",
                900L,
                604800L
        );
        tokenProvider = new JwtTokenProvider(props);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed")
                .fullName("Test User")
                .roles(Set.of(Role.CUSTOMER))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken() — produces valid JWT")
    void generateAccessToken_producesValidJwt() {
        String token = tokenProvider.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("extractUserId() — returns correct userId from token")
    void extractUserId_returnsCorrectUserId() {
        String token  = tokenProvider.generateAccessToken(testUser);
        String userId = tokenProvider.extractUserId(token);

        assertThat(userId).isEqualTo(testUser.getId().toString());
    }

    @Test
    @DisplayName("isValid() — returns false for tampered token")
    void isValid_tamperedToken_returnsFalse() {
        String token   = tokenProvider.generateAccessToken(testUser);
        String tampered = token + "tampered";

        assertThat(tokenProvider.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("getRemainingExpirySeconds() — returns positive value for fresh token")
    void getRemainingExpirySeconds_freshToken_returnsPositive() {
        String token     = tokenProvider.generateAccessToken(testUser);
        long   remaining = tokenProvider.getRemainingExpirySeconds(token);

        assertThat(remaining).isGreaterThan(0).isLessThanOrEqualTo(900L);
    }

    @Test
    @DisplayName("roles claim — contains user roles")
    void generateAccessToken_containsRolesClaim() {
        String token = tokenProvider.generateAccessToken(testUser);
        var    claims = tokenProvider.parseToken(token);

        assertThat(claims.get("roles", String.class)).contains("CUSTOMER");
    }
}
