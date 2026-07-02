package com.example.common.events.saga;

import lombok.Builder;
import lombok.Getter;

/**
 * Saga reply: Inventory Service → Orchestrator.
 * Confirms that compensation (stock release) completed.
 */
@Getter
@Builder
public class StockReleasedReply {

    private final String sagaId;
    private final String orderId;
    private final boolean success;
}
