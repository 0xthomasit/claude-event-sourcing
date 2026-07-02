package com.example.invoice.infrastructure.messaging.publisher;

import com.example.common.events.KafkaTopics;
import com.example.common.events.invoice.InvoiceIssuedEvent;
import com.example.common.events.invoice.InvoiceVoidedEvent;
import com.example.invoice.domain.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishIssued(Invoice invoice) {
        InvoiceIssuedEvent event = InvoiceIssuedEvent.builder()
                .aggregateId(invoice.getId().toString())
                .invoiceNumber(invoice.getInvoiceNumber())
                .orderId(invoice.getOrderId())
                .customerId(invoice.getCustomerId())
                .customerEmail(invoice.getCustomerEmail())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .occurredOn(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.INVOICE_ISSUED, invoice.getOrderId(), event);
        log.debug("Published InvoiceIssuedEvent: {}", invoice.getInvoiceNumber());
    }

    public void publishVoided(Invoice invoice) {
        InvoiceVoidedEvent event = InvoiceVoidedEvent.builder()
                .aggregateId(invoice.getId().toString())
                .invoiceNumber(invoice.getInvoiceNumber())
                .orderId(invoice.getOrderId())
                .reason(invoice.getVoidReason())
                .occurredOn(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.INVOICE_VOIDED, invoice.getOrderId(), event);
        log.debug("Published InvoiceVoidedEvent: {}", invoice.getInvoiceNumber());
    }
}
