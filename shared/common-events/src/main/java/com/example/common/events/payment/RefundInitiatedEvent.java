package com.example.common.events.payment;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundInitiatedEvent implements DomainEvent {

    private final String     aggregateId;
    private final String     orderId;
    private final BigDecimal amount;
    private final String     reason;
    private final Instant    occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "REFUND_INITIATED";
    }
}
