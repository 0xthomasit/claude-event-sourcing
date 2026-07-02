package com.example.review.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks which orders have been delivered — used to verify review eligibility.
 * Populated by listening to OrderDeliveredEvent.
 */
@Entity
@Table(name = "order_delivery_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDeliveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", unique = true, nullable = false, length = 36)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "product_ids", nullable = false, columnDefinition = "JSONB")
    private String productIds;   // JSON array of product IDs

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;
}
