package com.example.common.events.order;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderConfirmedEvent implements DomainEvent {

    private final String aggregateId;
    private final String customerId;
    private final BigDecimal totalAmount;
    private final List<OrderItemDto> items;
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "ORDER_CONFIRMED";
    }
}
