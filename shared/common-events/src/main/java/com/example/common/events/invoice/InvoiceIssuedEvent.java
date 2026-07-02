package com.example.common.events.invoice;

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
public class InvoiceIssuedEvent implements DomainEvent {

    private final String aggregateId;   // invoiceId
    private final String invoiceNumber;
    private final String orderId;
    private final String customerId;
    private final String customerEmail;
    private final BigDecimal totalAmount;
    private final String currency;
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "INVOICE_ISSUED";
    }
}
