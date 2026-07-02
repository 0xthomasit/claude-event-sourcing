package com.example.common.events.saga;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Saga command: Orchestrator → Inventory Service.
 * Requests stock reservation for all items in an order.
 */
@Getter
@Builder
public class ReserveStockCommand {

    private final String sagaId;
    private final String orderId;
    private final List<ItemToReserve> items;

    @Getter
    @Builder
    public static class ItemToReserve {
        private final String productId;
        private final int quantity;
    }
}
