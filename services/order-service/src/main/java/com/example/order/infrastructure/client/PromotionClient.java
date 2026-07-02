package com.example.order.infrastructure.client;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * HTTP client for calling promotion-service via Eureka service discovery.
 *
 * <p>Used BEFORE saga starts to validate and redeem a promotion code.
 * If redemption fails, the order is rejected immediately — no saga needed.
 *
 * <p>Design decision: promotion validation/redemption is a synchronous
 * pre-saga step because it's fast (<50ms) and failing here should
 * prevent the saga from starting at all (fail-fast).
 */
@Component
@Slf4j
public class PromotionClient {

    private final RestClient restClient;

    public PromotionClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://promotion-service")
                .build();
    }

    /**
     * Redeems a promotion code for an order.
     * Uses pessimistic locking in promotion-service to prevent race conditions.
     *
     * @return discount result or null if the call fails
     */
    public DiscountResult redeemPromotion(String code, String orderId,
                                           String customerId, BigDecimal orderAmount) {
        try {
            RedeemRequest request = new RedeemRequest(code, orderId, customerId, orderAmount);

            DiscountResult result = restClient.post()
                    .uri("/api/promotions/redeem")
                    .body(request)
                    .retrieve()
                    .body(DiscountResult.class);

            if (result != null && result.isValid()) {
                log.info("Promotion {} redeemed for order {}: discount={}",
                        code, orderId, result.getDiscountAmount());
            } else {
                log.warn("Promotion {} invalid for order {}: {}",
                        code, orderId, result != null ? result.getReason() : "null response");
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to call promotion-service for code {}: {}", code, e.getMessage());
            return DiscountResult.builder()
                    .valid(false)
                    .reason("Promotion service unavailable: " + e.getMessage())
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DiscountResult {
        private boolean valid;
        private String promotionCode;
        private BigDecimal discountAmount;
        private String discountType;
        private String reason;
    }

    private record RedeemRequest(String code, String orderId,
                                  String customerId, BigDecimal orderAmount) {}
}
