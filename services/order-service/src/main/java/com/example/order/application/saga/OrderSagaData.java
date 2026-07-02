package com.example.order.application.saga;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable data carrier persisted as JSONB in saga_instances.payload.
 * Contains all information the orchestrator needs to drive the saga forward
 * and to execute compensation when needed.
 *
 * <p>Uses @NoArgsConstructor for Jackson v3 deserialization compatibility
 * (Jackson v3 = tools.jackson package, @Jacksonized only works with v2).
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderSagaData {

    private String orderId;
    private String customerId;
    private List<ItemData> items;
    private BigDecimal totalAmount;
    private String currency;

    // ── Promotion tracking ───────────────────────────────────────────────────
    private String promotionCode;
    private BigDecimal discountAmount;
    private boolean promotionRedeemed;

    // ── Compensation tracking ────────────────────────────────────────────────
    private boolean stockReserved;
    private boolean paymentProcessed;
    private String paymentId;
    private String failureReason;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ItemData {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
    }

    // ── Copy helpers (immutable-style updates) ───────────────────────────────

    private OrderSagaDataBuilder copyBase() {
        return OrderSagaData.builder()
                .orderId(orderId).customerId(customerId).items(items)
                .totalAmount(totalAmount).currency(currency)
                .promotionCode(promotionCode).discountAmount(discountAmount)
                .promotionRedeemed(promotionRedeemed)
                .stockReserved(stockReserved)
                .paymentProcessed(paymentProcessed).paymentId(paymentId)
                .failureReason(failureReason);
    }

    public OrderSagaData withStockReserved(boolean reserved) {
        return copyBase().stockReserved(reserved).build();
    }

    public OrderSagaData withPaymentResult(boolean processed, String paymentId) {
        return copyBase().paymentProcessed(processed).paymentId(paymentId).build();
    }

    public OrderSagaData withFailureReason(String reason) {
        return copyBase().failureReason(reason).build();
    }

    public OrderSagaData withPromotion(String code, BigDecimal discount) {
        return copyBase()
                .promotionCode(code).discountAmount(discount).promotionRedeemed(true)
                .build();
    }
}

