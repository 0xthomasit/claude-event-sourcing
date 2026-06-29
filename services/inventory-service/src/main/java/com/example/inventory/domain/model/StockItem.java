package com.example.inventory.domain.model;

import com.example.common.domain.AggregateRoot;
import com.example.common.events.inventory.*;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * StockItem Aggregate Root — tracks available and reserved quantities for one product.
 *
 * State is fully reconstructed by replaying domain events.
 * Snapshot is taken every SNAPSHOT_THRESHOLD events to bound replay cost.
 */
@Getter
public class StockItem extends AggregateRoot {

    public static final int SNAPSHOT_THRESHOLD = 100;

    private UUID   id;
    private String productId;
    private String warehouseId;
    private int    availableQuantity;
    private int    reservedQuantity;
    private List<StockMovement> movements = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    // No-arg constructor for reconstitution
    public StockItem() {}

    // ─── Factory ─────────────────────────────────────────────────────────────

    public static StockItem initialize(String productId, String warehouseId, int initialQty) {
        if (productId == null || productId.isBlank())
            throw new IllegalArgumentException("productId must not be blank");
        if (initialQty < 0)
            throw new IllegalArgumentException("initialQty must be >= 0");

        StockItem item = new StockItem();
        item.id = UUID.randomUUID();

        StockReplenishedEvent event = StockReplenishedEvent.builder()
                .aggregateId(item.id.toString())
                .productId(productId)
                .quantity(initialQty)
                .reference("INITIAL")
                .occurredOn(Instant.now())
                .build();

        item.raiseEvent(event);
        item.apply(event);
        return item;
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    public void reserve(String orderId, int qty) {
        if (qty <= 0)
            throw new IllegalArgumentException("Reserve quantity must be > 0");
        if (availableQuantity < qty)
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s: available=%d, requested=%d",
                            productId, availableQuantity, qty));

        StockReservedEvent event = StockReservedEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .productId(productId)
                .quantity(qty)
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    public void release(String orderId, int qty) {
        if (qty <= 0)
            throw new IllegalArgumentException("Release quantity must be > 0");
        if (reservedQuantity < qty)
            throw new IllegalStateException(
                    String.format("Cannot release %d — only %d reserved for product %s",
                            qty, reservedQuantity, productId));

        StockReleasedEvent event = StockReleasedEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .productId(productId)
                .quantity(qty)
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    public void reduce(String orderId, int qty) {
        if (qty <= 0)
            throw new IllegalArgumentException("Reduce quantity must be > 0");
        if (reservedQuantity < qty)
            throw new IllegalStateException(
                    String.format("Cannot reduce %d — only %d reserved for product %s",
                            qty, reservedQuantity, productId));

        StockReducedEvent event = StockReducedEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .productId(productId)
                .quantity(qty)
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    public void replenish(int qty, String reference) {
        if (qty <= 0)
            throw new IllegalArgumentException("Replenish quantity must be > 0");

        StockReplenishedEvent event = StockReplenishedEvent.builder()
                .aggregateId(id.toString())
                .productId(productId)
                .quantity(qty)
                .reference(reference)
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    public void adjust(int delta, String reason) {
        int newQty = availableQuantity + delta;
        if (newQty < 0)
            throw new IllegalStateException(
                    String.format("Adjustment would result in negative stock: current=%d, delta=%d",
                            availableQuantity, delta));

        StockAdjustedEvent event = StockAdjustedEvent.builder()
                .aggregateId(id.toString())
                .productId(productId)
                .delta(delta)
                .quantityAfter(newQty)
                .reason(reason)
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    // ─── Event Apply (reconstitute state) ────────────────────────────────────

    public void apply(StockReplenishedEvent e) {
        if (this.id == null) {
            // First event — initialize aggregate
            this.id          = UUID.fromString(e.getAggregateId());
            this.productId   = e.getProductId();
            this.warehouseId = "DEFAULT";
            this.createdAt   = e.getOccurredOn();
        }
        this.availableQuantity += e.getQuantity();
        this.updatedAt = e.getOccurredOn();
        this.movements.add(StockMovement.of(
                StockMovementType.REPLENISH, e.getQuantity(), e.getReference(), null));
        incrementVersion();
    }

    public void apply(StockReservedEvent e) {
        this.availableQuantity -= e.getQuantity();
        this.reservedQuantity  += e.getQuantity();
        this.updatedAt = e.getOccurredOn();
        this.movements.add(StockMovement.of(
                StockMovementType.RESERVE, e.getQuantity(), e.getOrderId(), null));
        incrementVersion();
    }

    public void apply(StockReleasedEvent e) {
        this.reservedQuantity  -= e.getQuantity();
        this.availableQuantity += e.getQuantity();
        this.updatedAt = e.getOccurredOn();
        this.movements.add(StockMovement.of(
                StockMovementType.RELEASE, e.getQuantity(), e.getOrderId(), null));
        incrementVersion();
    }

    public void apply(StockReducedEvent e) {
        this.reservedQuantity -= e.getQuantity();
        this.updatedAt = e.getOccurredOn();
        this.movements.add(StockMovement.of(
                StockMovementType.REDUCE, e.getQuantity(), e.getOrderId(), null));
        incrementVersion();
    }

    public void apply(StockAdjustedEvent e) {
        this.availableQuantity = e.getQuantityAfter();
        this.updatedAt = e.getOccurredOn();
        int absQty = Math.abs(e.getDelta());
        if (absQty > 0) {
            this.movements.add(StockMovement.of(
                    StockMovementType.ADJUST, absQty, null, e.getReason()));
        }
        incrementVersion();
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public int getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }

    public boolean hasEnoughStock(int qty) {
        return availableQuantity >= qty;
    }

    public List<StockMovement> getMovements() {
        return Collections.unmodifiableList(movements);
    }
}