package com.example.order.application.saga;

/**
 * Defines all steps in the Order Placement Saga.
 *
 * Happy path: STARTED → RESERVING_STOCK → STOCK_RESERVED → PROCESSING_PAYMENT
 *             → PAYMENT_COMPLETED → CONFIRMING_ORDER → COMPLETED
 *
 * Compensation: PAYMENT_FAILED → COMPENSATING_STOCK → COMPENSATED
 * Early reject: STOCK_RESERVATION_FAILED → REJECTED
 */
public enum OrderSagaStep {

    // ── Forward path ──────────────────────────────────────────────────────────
    STARTED,
    RESERVING_STOCK,
    STOCK_RESERVED,
    PROCESSING_PAYMENT,
    PAYMENT_COMPLETED,
    CONFIRMING_ORDER,
    COMPLETED,

    // ── Failure / Compensation path ──────────────────────────────────────────
    STOCK_RESERVATION_FAILED,
    PAYMENT_FAILED,
    COMPENSATING_STOCK,
    COMPENSATED,
    REJECTED;

    public boolean isTerminal() {
        return this == COMPLETED || this == COMPENSATED || this == REJECTED;
    }
}
