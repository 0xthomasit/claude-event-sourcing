package com.example.order.infrastructure.messaging.projector;

import com.example.common.events.order.*;
import com.example.order.infrastructure.persistence.entity.OrderDocument;
import com.example.order.infrastructure.persistence.repository.MongoOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rebuilds the MongoDB Read Model from Domain Events.
 * Called synchronously after events are persisted to the Event Store.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProjector {

    private final MongoOrderRepository mongoRepo;

    public void on(OrderPlacedEvent event) {
        List<OrderDocument.OrderItemDocument> items = event.getItems().stream()
                .map(i -> OrderDocument.OrderItemDocument.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .currency(i.getCurrency())
                        .build())
                .toList();

        mongoRepo.save(OrderDocument.builder()
                .id(event.getAggregateId())
                .customerId(event.getCustomerId())
                .status("PENDING")
                .totalAmount(event.getTotalAmount())
                .currency("VND")
                .items(items)
                .createdAt(event.getOccurredOn())
                .updatedAt(event.getOccurredOn())
                .build());
        log.debug("Projected OrderPlacedEvent for order {}", event.getAggregateId());
    }

    public void on(OrderConfirmedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus("CONFIRMED");
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(OrderCancelledEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus("CANCELLED");
            doc.setCancellationReason(event.getReason());
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(OrderShippedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus("SHIPPED");
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(OrderDeliveredEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setStatus("DELIVERED");
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }
}
