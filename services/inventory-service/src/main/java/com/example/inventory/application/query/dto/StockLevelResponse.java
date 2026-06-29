package com.example.inventory.application.query.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class StockLevelResponse {
    private final String id;
    private final String productId;
    private final String warehouseId;
    private final int    availableQuantity;
    private final int    reservedQuantity;
    private final int    totalQuantity;
    private final Instant updatedAt;
}