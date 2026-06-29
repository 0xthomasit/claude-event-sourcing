package com.example.common.events.inventory;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockAdjustedEvent implements DomainEvent {

    private final String aggregateId;
    private final String productId;
    private final int delta;          // positive = add, negative = subtract
    private final int quantityAfter;
    private final String reason;      // "STOCKTAKE", "DAMAGE", "CORRECTION"
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() { return "STOCK_ADJUSTED"; }
}