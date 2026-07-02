package com.example.invoice.application.command.handler;

import com.example.invoice.domain.model.Invoice;
import com.example.invoice.domain.repository.InvoiceRepository;
import com.example.invoice.infrastructure.einvoice.EInvoiceProvider;
import com.example.invoice.infrastructure.messaging.publisher.InvoiceEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoidInvoiceHandler {

    private final InvoiceRepository    invoiceRepository;
    private final EInvoiceProvider     eInvoiceProvider;
    private final InvoiceEventPublisher eventPublisher;

    @Transactional
    public Invoice handle(UUID invoiceId, String reason) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        invoice.voidInvoice(reason);

        // Notify e-invoice provider
        try {
            eInvoiceProvider.voidInvoice(invoice);
        } catch (Exception e) {
            log.warn("E-invoice void failed for {}: {}", invoice.getInvoiceNumber(), e.getMessage());
        }

        Invoice saved = invoiceRepository.save(invoice);
        eventPublisher.publishVoided(saved);

        log.info("Invoice voided: number={}, reason={}", saved.getInvoiceNumber(), reason);
        return saved;
    }

    /**
     * Auto-void by orderId when an order is cancelled.
     */
    @Transactional
    public void handleByOrderId(String orderId, String reason) {
        invoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
            try {
                invoice.voidInvoice(reason);
                eInvoiceProvider.voidInvoice(invoice);
                invoiceRepository.save(invoice);
                eventPublisher.publishVoided(invoice);
                log.info("Invoice auto-voided for order {}: {}", orderId, invoice.getInvoiceNumber());
            } catch (IllegalStateException e) {
                log.warn("Cannot void invoice for order {}: {}", orderId, e.getMessage());
            }
        });
    }
}
