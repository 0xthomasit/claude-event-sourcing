package com.example.payment.infrastructure.persistence.adapter;

import com.example.common.domain.DomainEvent;
import com.example.common.events.payment.*;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import com.example.payment.infrastructure.persistence.entity.EventStoreEntry;
import com.example.payment.infrastructure.persistence.repository.JpaEventStoreRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter — implements domain PaymentRepository port using JPA Event Store.
 * Persists events (append-only), reconstitutes aggregate by replaying events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaEventStoreRepository eventStoreRepo;
    private final ObjectMapper            objectMapper;

    @Override
    @Transactional
    public void save(Payment payment) {
        List<DomainEvent> uncommitted = payment.getUncommittedEvents();
        if (uncommitted.isEmpty()) return;

        Long maxSeq = eventStoreRepo.findMaxSequenceNumber(payment.getId().toString());
        long nextSeq = (maxSeq == null ? 0L : maxSeq) + 1;

        for (DomainEvent event : uncommitted) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                EventStoreEntry entry = EventStoreEntry.builder()
                        .aggregateId(payment.getId().toString())
                        .aggregateType("Payment")
                        .eventType(event.getEventType())
                        .eventVersion(event.getEventVersion())
                        .sequenceNumber(nextSeq++)
                        .payload(payload)
                        .occurredOn(event.getOccurredOn())
                        .build();
                eventStoreRepo.save(entry);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize event: " + event.getEventType(), e);
            }
        }
        payment.clearUncommittedEvents();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        List<EventStoreEntry> entries =
                eventStoreRepo.findByAggregateIdOrderBySequenceNumberAsc(id.toString());
        if (entries.isEmpty()) return Optional.empty();
        return Optional.of(reconstitute(entries));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(String orderId) {
        // Query event store for PaymentInitiatedEvent with matching orderId
        // For simplicity, we scan — in production add a lookup table or index
        return eventStoreRepo.findAll().stream()
                .filter(e -> e.getEventType().equals("PAYMENT_INITIATED"))
                .filter(e -> {
                    try {
                        PaymentInitiatedEvent evt = objectMapper.readValue(
                                e.getPayload(), PaymentInitiatedEvent.class);
                        return orderId.equals(evt.getOrderId());
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .findFirst()
                .map(e -> findById(UUID.fromString(e.getAggregateId())))
                .flatMap(opt -> opt);
    }

    // ─── Reconstitute aggregate from event stream ─────────────────────────────

    private Payment reconstitute(List<EventStoreEntry> entries) {
        Payment payment = new Payment();
        for (EventStoreEntry entry : entries) {
            try {
                applyEvent(payment, entry);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to deserialize event: " + entry.getEventType(), e);
            }
        }
        return payment;
    }

    private void applyEvent(Payment payment, EventStoreEntry entry) throws Exception {
        switch (entry.getEventType()) {
            case "PAYMENT_INITIATED" ->
                payment.apply(objectMapper.readValue(entry.getPayload(), PaymentInitiatedEvent.class));
            case "PAYMENT_PROCESSING" ->
                payment.apply(objectMapper.readValue(entry.getPayload(), PaymentProcessingEvent.class));
            case "PAYMENT_COMPLETED" ->
                payment.apply(objectMapper.readValue(entry.getPayload(), PaymentCompletedEvent.class));
            case "PAYMENT_FAILED" ->
                payment.apply(objectMapper.readValue(entry.getPayload(), PaymentFailedEvent.class));
            case "REFUND_INITIATED" ->
                payment.apply(objectMapper.readValue(entry.getPayload(), RefundInitiatedEvent.class));
            case "REFUND_COMPLETED" ->
                payment.apply(objectMapper.readValue(entry.getPayload(), RefundCompletedEvent.class));
            default ->
                log.warn("Unknown event type during reconstitution: {}", entry.getEventType());
        }
    }
}
