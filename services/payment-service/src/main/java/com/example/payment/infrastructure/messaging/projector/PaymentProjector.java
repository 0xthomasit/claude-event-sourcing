package com.example.payment.infrastructure.messaging.projector;

import com.example.common.events.payment.*;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.infrastructure.persistence.entity.PaymentDocument;
import com.example.common.domain.DomainEvent;
import com.example.payment.infrastructure.persistence.repository.MongoPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds and updates the MongoDB Read Model from domain events.
 * Called synchronously after events are persisted to Event Store.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProjector {

    private final MongoPaymentRepository mongoRepo;

    public void projectAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            if (event instanceof PaymentInitiatedEvent e) on(e);
            else if (event instanceof PaymentProcessingEvent e) on(e);
            else if (event instanceof PaymentCompletedEvent e) on(e);
            else if (event instanceof PaymentFailedEvent e) on(e);
            else if (event instanceof RefundInitiatedEvent e) on(e);
            else if (event instanceof RefundCompletedEvent e) on(e);
        }
    }

    public void on(PaymentInitiatedEvent event) {
        PaymentDocument doc = PaymentDocument.builder()
                .id(event.getAggregateId())
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .paymentMethodType(event.getPaymentMethodType())
                .maskedAccount(event.getMaskedAccount())
                .status(PaymentStatus.PENDING)
                .createdAt(event.getOccurredOn())
                .updatedAt(event.getOccurredOn())
                .build();
        mongoRepo.save(doc);
        log.debug("Projected PaymentInitiated: {}", event.getAggregateId());
    }

    public void on(PaymentProcessingEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus(PaymentStatus.PROCESSING);
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(PaymentCompletedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus(PaymentStatus.COMPLETED);
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
        log.debug("Projected PaymentCompleted: {}", event.getAggregateId());
    }

    public void on(PaymentFailedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus(PaymentStatus.FAILED);
            doc.setFailureReason(event.getFailureReason());
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
        log.debug("Projected PaymentFailed: {}", event.getAggregateId());
    }

    public void on(RefundInitiatedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus(PaymentStatus.REFUND_INITIATED);
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(RefundCompletedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus(PaymentStatus.REFUNDED);
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
        log.debug("Projected RefundCompleted: {}", event.getAggregateId());
    }
}
