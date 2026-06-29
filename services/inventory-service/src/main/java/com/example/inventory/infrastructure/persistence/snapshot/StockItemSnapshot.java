package com.example.inventory.infrastructure.persistence.snapshot;

import lombok.*;

/**
 * Serializable snapshot DTO — stored as JSONB in stock_snapshots table.
 * Contains only the mutable state fields of StockItem.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItemSnapshot {
    private String id;
    private String productId;
    private String warehouseId;
    private int    availableQuantity;
    private int    reservedQuantity;
    private long   version;
    private String createdAt;
    private String updatedAt;
}