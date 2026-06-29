package com.example.payment.infrastructure.persistence.entity;

import com.example.payment.domain.model.PaymentStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MongoDB Read Model — denormalized view of Payment for fast queries.
 * Rebuilt by PaymentProjector from domain events.
 */
@Document(collection = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderId;

    @Indexed
    private String customerId;

    private BigDecimal amount;
    private String     currency;
    private String     paymentMethodType;
    private String     maskedAccount;

    private PaymentStatus status;
    private String        failureReason;
    private String        transactionRef;

    private Instant createdAt;
    private Instant updatedAt;
}
