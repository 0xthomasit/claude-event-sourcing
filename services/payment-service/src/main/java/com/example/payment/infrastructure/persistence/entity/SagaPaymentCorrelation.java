package com.example.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Correlates a paymentId with its sagaId so that the async gateway callback
 * can route the completion/failure reply back to the correct saga.
 */
@Entity
@Table(name = "saga_payment_correlation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SagaPaymentCorrelation {

    @Id
    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "saga_id", nullable = false, length = 36)
    private String sagaId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
