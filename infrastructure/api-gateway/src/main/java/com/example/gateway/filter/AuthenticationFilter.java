package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * API Gateway filter: validates JWT, then injects X-User-Id and X-User-Roles
 * headers for downstream services. Downstream services NEVER see raw JWT —
 * they only trust these injected headers.
 */
@Component
@Slf4j
public class AuthenticationFilter
        extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.isValid(token)) {
                return unauthorized(exchange, "Invalid or expired JWT token");
            }

            String userId = jwtUtil.extractUserId(token);
            String roles  = jwtUtil.extractRoles(token);

            // Inject trusted headers — downstream services read these
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-User-Id",    userId);
                        headers.set("X-User-Roles", roles);
                        headers.remove(HttpHeaders.AUTHORIZATION); // strip raw JWT
                    }))
                    .build();

            log.debug("Authenticated userId={} roles={}", userId, roles);
            return chain.filter(mutated);
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.warn("Unauthorized request: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // No config needed for now
    }
}
