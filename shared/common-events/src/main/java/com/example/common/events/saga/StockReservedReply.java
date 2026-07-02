package com.example.common.events.saga;

import lombok.Builder;
import lombok.Getter;

/**
 * Saga reply: Inventory Service → Orchestrator.
 * Indicates whether stock reservation succeeded or failed.
 */
@Getter
@Builder
public class StockReservedReply {

    private final String sagaId;
    private final String orderId;
    private final boolean success;
    private final String failureReason;
}
