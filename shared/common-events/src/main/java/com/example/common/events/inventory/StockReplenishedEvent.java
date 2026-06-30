package com.example.common.events.inventory;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockReplenishedEvent implements DomainEvent {

    private final String aggregateId;
    private final String productId;
    private final int quantity;
    private final String reference;   // PO number, supplier ref, etc.
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() { return "STOCK_REPLENISHED"; }
}
