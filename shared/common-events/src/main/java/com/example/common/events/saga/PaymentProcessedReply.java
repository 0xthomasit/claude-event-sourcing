package com.example.common.events.saga;

import lombok.Builder;
import lombok.Getter;

/**
 * Saga reply: Payment Service → Orchestrator.
 * Indicates whether payment processing succeeded or failed.
 */
@Getter
@Builder
public class PaymentProcessedReply {

    private final String sagaId;
    private final String orderId;
    private final boolean success;
    private final String paymentId;
    private final String failureReason;
}
