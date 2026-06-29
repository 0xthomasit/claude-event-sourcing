package com.example.auth.application.service;

import com.example.auth.application.dto.*;
import com.example.auth.domain.model.Role;
import com.example.auth.domain.model.User;
import com.example.auth.domain.repository.UserRepository;
import com.example.auth.infrastructure.security.JwtProperties;
import com.example.auth.infrastructure.security.JwtTokenProvider;
import com.example.auth.infrastructure.security.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtTokenProvider  jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties     jwtProperties;

    // ─── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roles(Set.of(Role.CUSTOMER))
                .enabled(true)
                .build());

        log.info("User registered: {} ({})", user.getEmail(), user.getId());
        return issueTokenPair(user);
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled())
            throw new BadCredentialsException("Account is disabled");

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid email or password");

        log.info("User logged in: {}", user.getEmail());
        return issueTokenPair(user);
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        String userId = refreshTokenStore.validate(request.getRefreshToken());
        if (userId == null)
            throw new BadCredentialsException("Invalid or expired refresh token");

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.isEnabled())
            throw new BadCredentialsException("Account is disabled");

        // Rotate: invalidate old refresh token, issue new one
        String newAccessToken  = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = refreshTokenStore.issue(userId);
        refreshTokenStore.revoke(request.getRefreshToken());

        log.info("Token refreshed for user: {}", user.getEmail());
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.accessTokenExpirySeconds())
                .build();
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    public void logout(String accessToken, LogoutRequest request) {
        // 1. Blacklist the access token for its remaining TTL
        long remaining = jwtTokenProvider.getRemainingExpirySeconds(accessToken);
        refreshTokenStore.blacklistAccessToken(accessToken, remaining);

        // 2. Revoke the refresh token
        refreshTokenStore.revoke(request.getRefreshToken());

        log.info("User logged out, tokens revoked");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private TokenResponse issueTokenPair(User user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = refreshTokenStore.issue(user.getId().toString());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.accessTokenExpirySeconds())
                .build();
    }
}