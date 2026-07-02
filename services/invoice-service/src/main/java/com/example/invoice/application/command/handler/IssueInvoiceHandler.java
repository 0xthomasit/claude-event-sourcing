package com.example.invoice.application.command.handler;

import com.example.common.events.order.OrderConfirmedEvent;
import com.example.common.events.order.OrderItemDto;
import com.example.invoice.domain.model.Invoice;
import com.example.invoice.domain.repository.InvoiceRepository;
import com.example.invoice.domain.service.InvoiceNumberGenerator;
import com.example.invoice.infrastructure.einvoice.EInvoiceProvider;
import com.example.invoice.infrastructure.messaging.publisher.InvoiceEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Issues an invoice from an OrderConfirmedEvent.
 *
 * <p>Idempotent by orderId: returns existing invoice if already issued.
 * Integrates with external e-invoice provider for legal compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueInvoiceHandler {

    private final InvoiceRepository      invoiceRepository;
    private final InvoiceNumberGenerator  numberGenerator;
    private final EInvoiceProvider        eInvoiceProvider;
    private final InvoiceEventPublisher   eventPublisher;

    @Transactional
    public Invoice handle(OrderConfirmedEvent event) {
        // Idempotency — check if invoice already exists for this order
        var existing = invoiceRepository.findByOrderId(event.getAggregateId());
        if (existing.isPresent()) {
            log.info("Invoice already exists for order {}: {}",
                    event.getAggregateId(), existing.get().getInvoiceNumber());
            return existing.get();
        }

        // Build invoice from order data
        Invoice invoice = Invoice.builder()
                .orderId(event.getAggregateId())
                .customerId(event.getCustomerId())
                .currency("VND")
                .build();

        // Add line items from order
        if (event.getItems() != null) {
            for (OrderItemDto item : event.getItems()) {
                invoice.addLineItem(
                        item.getProductId(),
                        item.getProductName(),
                        null,   // SKU — not in order event, could be enriched
                        item.getQuantity(),
                        item.getUnitPrice());
            }
        }

        // Generate invoice number and issue
        String invoiceNumber = numberGenerator.generateNext();
        invoice.issue(invoiceNumber);

        // Send to e-invoice provider
        try {
            String eInvoiceRef = eInvoiceProvider.issue(invoice);
            invoice.setEInvoiceRef(eInvoiceRef);
            log.info("E-invoice issued: {} → ref={}", invoiceNumber, eInvoiceRef);
        } catch (Exception e) {
            log.warn("E-invoice provider call failed for {}: {} — invoice saved without ref",
                    invoiceNumber, e.getMessage());
            // Invoice is still valid internally, e-invoice can be retried
        }

        Invoice saved = invoiceRepository.save(invoice);

        // Publish event
        eventPublisher.publishIssued(saved);

        log.info("Invoice issued: number={}, order={}, total={}",
                saved.getInvoiceNumber(), saved.getOrderId(), saved.getTotalAmount());

        return saved;
    }
}
