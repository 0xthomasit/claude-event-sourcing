package com.example.payment.domain.model;

/**
 * Payment lifecycle states.
 *
 * PENDING → PROCESSING → COMPLETED
 *                      → FAILED
 * COMPLETED → REFUNDED (via RefundInitiated → RefundCompleted)
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUND_INITIATED,
    REFUNDED
}
