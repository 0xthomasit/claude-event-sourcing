package com.example.common.events.payment;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentInitiatedEvent implements DomainEvent {

    private final String     aggregateId;
    private final String     orderId;
    private final String     customerId;
    private final BigDecimal amount;
    private final String     currency;
    private final String     paymentMethodType;
    private final String     maskedAccount;
    private final Instant    occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "PAYMENT_INITIATED";
    }
}
