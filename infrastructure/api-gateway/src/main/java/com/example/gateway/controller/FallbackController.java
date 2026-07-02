package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller invoked by the CircuitBreaker gateway filter
 * when a downstream service is unavailable or too slow.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<Map<String, Object>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String serviceName = extractServiceName(path);

        return Mono.just(Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", "The " + serviceName + " is temporarily unavailable. Please try again later.",
                "path", path,
                "timestamp", Instant.now().toString()
        ));
    }

    private String extractServiceName(String path) {
        // /api/orders/... → order-service
        if (path.startsWith("/api/")) {
            String segment = path.substring(5); // remove "/api/"
            int slashIdx = segment.indexOf('/');
            if (slashIdx > 0) {
                segment = segment.substring(0, slashIdx);
            }
            return segment + "-service";
        }
        return "downstream service";
    }
}
