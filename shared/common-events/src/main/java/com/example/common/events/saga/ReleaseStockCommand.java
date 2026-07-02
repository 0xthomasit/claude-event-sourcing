package com.example.common.events.saga;

import lombok.Builder;
import lombok.Getter;

/**
 * Saga compensation command: Orchestrator → Inventory Service.
 * Releases previously reserved stock when a downstream step fails.
 */
@Getter
@Builder
public class ReleaseStockCommand {

    private final String sagaId;
    private final String orderId;
}
