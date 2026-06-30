package com.example.inventory.infrastructure.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Read Model — fast queries for stock levels.
 * Rebuilt from domain events by StockItemProjector.
 */
@Document(collection = "stock_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockItemDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String productId;

    private String warehouseId;
    private int    availableQuantity;
    private int    reservedQuantity;
    private int    totalQuantity;
    private Instant updatedAt;
}
