package com.example.inventory.infrastructure.messaging.projector;

import com.example.common.events.inventory.*;
import com.example.inventory.infrastructure.persistence.entity.StockItemDocument;
import com.example.inventory.infrastructure.persistence.repository.MongoStockItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Rebuilds the MongoDB Read Model from domain events.
 * Called synchronously after events are persisted to the Event Store.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockItemProjector {

    private final MongoStockItemRepository mongoRepo;

    public void on(StockReplenishedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresentOrElse(
                doc -> {
                    doc.setAvailableQuantity(doc.getAvailableQuantity() + event.getQuantity());
                    doc.setTotalQuantity(doc.getAvailableQuantity() + doc.getReservedQuantity());
                    doc.setUpdatedAt(event.getOccurredOn());
                    mongoRepo.save(doc);
                },
                () -> mongoRepo.save(StockItemDocument.builder()
                        .id(event.getAggregateId())
                        .productId(event.getProductId())
                        .warehouseId("DEFAULT")
                        .availableQuantity(event.getQuantity())
                        .reservedQuantity(0)
                        .totalQuantity(event.getQuantity())
                        .updatedAt(event.getOccurredOn())
                        .build())
        );
        log.debug("Projected StockReplenishedEvent for {}", event.getAggregateId());
    }

    public void on(StockReservedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setAvailableQuantity(doc.getAvailableQuantity() - event.getQuantity());
            doc.setReservedQuantity(doc.getReservedQuantity() + event.getQuantity());
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(StockReleasedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setReservedQuantity(doc.getReservedQuantity() - event.getQuantity());
            doc.setAvailableQuantity(doc.getAvailableQuantity() + event.getQuantity());
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(StockReducedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setReservedQuantity(doc.getReservedQuantity() - event.getQuantity());
            doc.setTotalQuantity(doc.getAvailableQuantity() + doc.getReservedQuantity());
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }

    public void on(StockAdjustedEvent event) {
        mongoRepo.findById(event.getAggregateId()).ifPresent(doc -> {
            doc.setAvailableQuantity(event.getQuantityAfter());
            doc.setTotalQuantity(event.getQuantityAfter() + doc.getReservedQuantity());
            doc.setUpdatedAt(event.getOccurredOn());
            mongoRepo.save(doc);
        });
    }
}
