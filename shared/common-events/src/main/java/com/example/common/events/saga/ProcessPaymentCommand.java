package com.example.common.events.saga;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Saga command: Orchestrator → Payment Service.
 * Requests payment processing for an order.
 */
@Getter
@Builder
public class ProcessPaymentCommand {

    private final String sagaId;
    private final String orderId;
    private final String customerId;
    private final BigDecimal amount;
    private final String currency;
}
