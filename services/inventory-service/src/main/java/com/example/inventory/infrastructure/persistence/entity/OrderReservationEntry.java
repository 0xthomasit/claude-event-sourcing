package com.example.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Tracks which StockItem aggregates were reserved for each orderId.
 * Enables efficient handleByOrderId() without scanning all events.
 */
@Entity
@Table(name = "order_reservations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderReservationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id",     nullable = false, length = 36)
    private String orderId;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;   // StockItem aggregate ID

    @Column(name = "product_id",   nullable = false, length = 36)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "RESERVED";   // RESERVED | RELEASED | REDUCED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
