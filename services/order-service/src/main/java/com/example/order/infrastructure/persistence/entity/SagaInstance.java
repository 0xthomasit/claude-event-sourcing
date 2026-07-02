package com.example.order.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the saga_instances table.
 * Stores the current state of each saga execution.
 */
@Entity
@Table(name = "saga_instances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SagaInstance {

    @Id
    private UUID id;

    @Column(name = "saga_type", nullable = false, length = 100)
    private String sagaType;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "current_step", nullable = false, length = 50)
    private String currentStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason")
    private String failureReason;
}
