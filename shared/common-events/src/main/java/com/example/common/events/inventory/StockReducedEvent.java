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
public class StockReducedEvent implements DomainEvent {

    private final String aggregateId;   // stockItemId
    private final String orderId;
    private final String productId;
    private final int quantity;
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "STOCK_REDUCED";
    }
}
