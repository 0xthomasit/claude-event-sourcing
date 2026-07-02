package com.example.common.events.invoice;

import com.example.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceVoidedEvent implements DomainEvent {

    private final String aggregateId;   // invoiceId
    private final String invoiceNumber;
    private final String orderId;
    private final String reason;
    private final Instant occurredOn;

    @Builder.Default
    private final int eventVersion = 1;

    @Override
    public String getEventType() {
        return "INVOICE_VOIDED";
    }
}
