package com.example.common.security;

/**
 * Shared security constants used across all services.
 * API Gateway injects these headers after JWT validation —
 * downstream services trust these headers (never exposed to clients).
 */
public final class SecurityConstants {

    // Headers injected by API Gateway after JWT validation
    public static final String HEADER_USER_ID    = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    // JWT Bearer prefix
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_HEADER   = "Authorization";

    // Role names
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SELLER   = "SELLER";
    public static final String ROLE_ADMIN    = "ADMIN";

    private SecurityConstants() {}
}
