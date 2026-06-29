package com.example.order.application.query.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class OrderResponse {
    private final String id;
    private final String customerId;
    private final String status;
    private final BigDecimal totalAmount;
    private final String currency;
    private final List<OrderItemResponse> items;
    private final String cancellationReason;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Getter @Builder
    public static class OrderItemResponse {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final String currency;
    }
}