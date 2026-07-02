package com.example.common.events.saga;

/**
 * Centralized Kafka topic constants for Saga Orchestration.
 *
 * Convention: saga.{service}.{action} for commands,
 *             saga.{service}.{action}.reply for replies.
 */
public final class SagaTopics {

    // ── Inventory ────────────────────────────────────────────────────────────
    public static final String RESERVE_STOCK_CMD   = "saga.inventory.reserve-stock";
    public static final String RESERVE_STOCK_REPLY = "saga.inventory.reserve-stock.reply";
    public static final String RELEASE_STOCK_CMD   = "saga.inventory.release-stock";
    public static final String RELEASE_STOCK_REPLY = "saga.inventory.release-stock.reply";

    // ── Payment ──────────────────────────────────────────────────────────────
    public static final String PROCESS_PAYMENT_CMD   = "saga.payment.process";
    public static final String PROCESS_PAYMENT_REPLY = "saga.payment.process.reply";

    private SagaTopics() {}
}
