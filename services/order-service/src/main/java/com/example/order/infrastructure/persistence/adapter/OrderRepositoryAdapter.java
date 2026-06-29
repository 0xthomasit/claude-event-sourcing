package com.example.order.infrastructure.persistence.adapter;

import com.example.common.domain.DomainEvent;
import com.example.common.events.order.*;
import com.example.order.domain.model.Order;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.persistence.entity.EventStoreEntry;
import com.example.order.infrastructure.persistence.repository.JpaEventStoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRepositoryAdapter implements OrderRepository {

    private final JpaEventStoreRepository eventStoreRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void save(Order order) {
        long currentCount = eventStoreRepo.countByAggregateId(order.getId().toString());
        List<DomainEvent> uncommitted = order.getUncommittedEvents();

        for (int i = 0; i < uncommitted.size(); i++) {
            DomainEvent event = uncommitted.get(i);
            EventStoreEntry entry = EventStoreEntry.builder()
                    .aggregateId(event.getAggregateId())
                    .aggregateType("Order")
                    .eventType(event.getEventType())
                    .eventVersion(event.getEventVersion())
                    .sequenceNumber(currentCount + i + 1)
                    .payload(serialize(event))
                    .occurredOn(event.getOccurredOn())
                    .build();
            eventStoreRepo.save(entry);
        }
        order.clearUncommittedEvents();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        List<EventStoreEntry> entries =
                eventStoreRepo.findByAggregateIdOrderBySequenceNumberAsc(id.toString());
        if (entries.isEmpty()) return Optional.empty();

        Order order = new Order();
        for (EventStoreEntry entry : entries) {
            applyEvent(order, entry);
        }
        return Optional.of(order);
    }

    private void applyEvent(Order order, EventStoreEntry entry) {
        try {
            switch (entry.getEventType()) {
                case "ORDER_PLACED"    -> order.apply(deserialize(entry.getPayload(), OrderPlacedEvent.class));
                case "ORDER_CONFIRMED" -> order.apply(deserialize(entry.getPayload(), OrderConfirmedEvent.class));
                case "ORDER_CANCELLED" -> order.apply(deserialize(entry.getPayload(), OrderCancelledEvent.class));
                case "ORDER_SHIPPED"   -> order.apply(deserialize(entry.getPayload(), OrderShippedEvent.class));
                case "ORDER_DELIVERED" -> order.apply(deserialize(entry.getPayload(), OrderDeliveredEvent.class));
                default -> log.warn("Unknown event type: {}", entry.getEventType());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply event: " + entry.getEventType(), e);
        }
    }

    private String serialize(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException("Serialize failed", e); }
    }
    private <T> T deserialize(String json, Class<T> clazz) {
        try { return objectMapper.readValue(json, clazz); }
        catch (Exception e) { throw new RuntimeException("Deserialize failed for " + clazz.getSimpleName(), e); }
    }
}
