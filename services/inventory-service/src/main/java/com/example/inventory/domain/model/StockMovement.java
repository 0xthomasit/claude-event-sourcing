package com.example.inventory.domain.model;

import com.example.common.domain.ValueObject;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable value object — records a single stock movement.
 * Stored inside the StockItem aggregate as part of its history.
 */
@Getter
public final class StockMovement implements ValueObject {

    private final StockMovementType type;
    private final int quantity;
    private final String reference;   // orderId, PO number, etc.
    private final String reason;
    private final Instant occurredOn;

    private StockMovement(StockMovementType type, int quantity,
                          String reference, String reason, Instant occurredOn) {
        if (quantity <= 0)
            throw new IllegalArgumentException("StockMovement quantity must be > 0");
        this.type       = type;
        this.quantity   = quantity;
        this.reference  = reference;
        this.reason     = reason;
        this.occurredOn = occurredOn;
    }

    public static StockMovement of(StockMovementType type, int quantity,
                                   String reference, String reason) {
        return new StockMovement(type, quantity, reference, reason, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockMovement m)) return false;
        return quantity == m.quantity
                && type == m.type
                && Objects.equals(reference, m.reference)
                && Objects.equals(occurredOn, m.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, quantity, reference, occurredOn);
    }
}