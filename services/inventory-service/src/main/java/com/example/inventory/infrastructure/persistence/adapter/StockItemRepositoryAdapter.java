package com.example.inventory.infrastructure.persistence.adapter;

import com.example.common.domain.DomainEvent;
import com.example.common.events.inventory.*;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.repository.StockItemRepository;
import com.example.inventory.infrastructure.persistence.entity.EventStoreEntry;
import com.example.inventory.infrastructure.persistence.entity.StockSnapshotEntry;
import com.example.inventory.infrastructure.persistence.repository.JpaEventStoreRepository;
import com.example.inventory.infrastructure.persistence.repository.JpaSnapshotRepository;
import com.example.inventory.infrastructure.persistence.snapshot.StockItemSnapshot;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockItemRepositoryAdapter implements StockItemRepository {

    private final JpaEventStoreRepository eventStoreRepo;
    private final JpaSnapshotRepository   snapshotRepo;
    private final ObjectMapper            objectMapper;

    @Override
    @Transactional
    public void save(StockItem stockItem) {
        long currentCount = eventStoreRepo.countByAggregateId(stockItem.getId().toString());
        List<DomainEvent> uncommitted = stockItem.getUncommittedEvents();

        for (int i = 0; i < uncommitted.size(); i++) {
            DomainEvent event = uncommitted.get(i);
            EventStoreEntry entry = EventStoreEntry.builder()
                    .aggregateId(event.getAggregateId())
                    .aggregateType("StockItem")
                    .eventType(event.getEventType())
                    .eventVersion(event.getEventVersion())
                    .sequenceNumber(currentCount + i + 1)
                    .payload(serialize(event))
                    .occurredOn(event.getOccurredOn())
                    .build();
            eventStoreRepo.save(entry);
        }

        long newTotal = currentCount + uncommitted.size();
        stockItem.clearUncommittedEvents();

        // Take snapshot every SNAPSHOT_THRESHOLD events
        if (newTotal % StockItem.SNAPSHOT_THRESHOLD == 0 && newTotal > 0) {
            takeSnapshot(stockItem, newTotal);
            log.info("Snapshot taken for StockItem {} at sequence {}", stockItem.getId(), newTotal);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockItem> findById(UUID id) {
        return reconstitute(id.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockItem> findByProductId(String productId) {
        // Find aggregateId from event store by scanning first STOCK_REPLENISHED event
        // In production, maintain a product→aggregateId index in MongoDB read model
        List<EventStoreEntry> all = eventStoreRepo.findAll();
        return all.stream()
                .filter(e -> e.getEventType().equals("STOCK_REPLENISHED"))
                .filter(e -> {
                    try {
                        StockReplenishedEvent ev = deserialize(e.getPayload(), StockReplenishedEvent.class);
                        return productId.equals(ev.getProductId());
                    } catch (Exception ex) { return false; }
                })
                .findFirst()
                .flatMap(e -> reconstitute(e.getAggregateId()));
    }

    // ─── Snapshot + Reconstitution ────────────────────────────────────────────

    private Optional<StockItem> reconstitute(String aggregateId) {
        // 1. Load latest snapshot (if any)
        Optional<StockSnapshotEntry> snapshotEntry = snapshotRepo.findById(aggregateId);
        long fromSequence = 0L;
        StockItem stockItem = new StockItem();

        if (snapshotEntry.isPresent()) {
            StockItemSnapshot snap = deserialize(snapshotEntry.get().getSnapshotData(),
                    StockItemSnapshot.class);
            restoreFromSnapshot(stockItem, snap);
            fromSequence = snapshotEntry.get().getSequenceNumber();
            log.debug("Restored StockItem {} from snapshot at seq {}", aggregateId, fromSequence);
        }

        // 2. Load only events AFTER snapshot sequence — max SNAPSHOT_THRESHOLD events
        List<EventStoreEntry> entries = fromSequence == 0
                ? eventStoreRepo.findByAggregateIdOrderBySequenceNumberAsc(aggregateId)
                : eventStoreRepo.findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                        aggregateId, fromSequence);

        if (entries.isEmpty() && snapshotEntry.isEmpty()) return Optional.empty();

        // 3. Replay remaining events
        for (EventStoreEntry entry : entries) {
            applyEvent(stockItem, entry);
        }

        return Optional.of(stockItem);
    }

    private void restoreFromSnapshot(StockItem stockItem, StockItemSnapshot snap) {
        try {
            var idField = StockItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(stockItem, UUID.fromString(snap.getId()));

            var productIdField = StockItem.class.getDeclaredField("productId");
            productIdField.setAccessible(true);
            productIdField.set(stockItem, snap.getProductId());

            var warehouseIdField = StockItem.class.getDeclaredField("warehouseId");
            warehouseIdField.setAccessible(true);
            warehouseIdField.set(stockItem, snap.getWarehouseId());

            var availableField = StockItem.class.getDeclaredField("availableQuantity");
            availableField.setAccessible(true);
            availableField.set(stockItem, snap.getAvailableQuantity());

            var reservedField = StockItem.class.getDeclaredField("reservedQuantity");
            reservedField.setAccessible(true);
            reservedField.set(stockItem, snap.getReservedQuantity());

            var versionField = com.example.common.domain.AggregateRoot.class.getDeclaredField("version");
            versionField.setAccessible(true);
            versionField.set(stockItem, snap.getVersion());
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore StockItem from snapshot", e);
        }
    }

    private void applyEvent(StockItem stockItem, EventStoreEntry entry) {
        try {
            switch (entry.getEventType()) {
                case "STOCK_REPLENISHED" -> stockItem.apply(deserialize(entry.getPayload(), StockReplenishedEvent.class));
                case "STOCK_RESERVED"    -> stockItem.apply(deserialize(entry.getPayload(), StockReservedEvent.class));
                case "STOCK_RELEASED"    -> stockItem.apply(deserialize(entry.getPayload(), StockReleasedEvent.class));
                case "STOCK_REDUCED"     -> stockItem.apply(deserialize(entry.getPayload(), StockReducedEvent.class));
                case "STOCK_ADJUSTED"    -> stockItem.apply(deserialize(entry.getPayload(), StockAdjustedEvent.class));
                default -> log.warn("Unknown event type: {}", entry.getEventType());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply event: " + entry.getEventType(), e);
        }
    }

    private void takeSnapshot(StockItem stockItem, long sequenceNumber) {
        StockItemSnapshot snap = StockItemSnapshot.builder()
                .id(stockItem.getId().toString())
                .productId(stockItem.getProductId())
                .warehouseId(stockItem.getWarehouseId())
                .availableQuantity(stockItem.getAvailableQuantity())
                .reservedQuantity(stockItem.getReservedQuantity())
                .version(stockItem.getVersion())
                .createdAt(stockItem.getCreatedAt() != null ? stockItem.getCreatedAt().toString() : null)
                .updatedAt(Instant.now().toString())
                .build();

        snapshotRepo.save(StockSnapshotEntry.builder()
                .aggregateId(stockItem.getId().toString())
                .aggregateType("StockItem")
                .snapshotData(serialize(snap))
                .sequenceNumber(sequenceNumber)
                .createdAt(Instant.now())
                .build());
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
