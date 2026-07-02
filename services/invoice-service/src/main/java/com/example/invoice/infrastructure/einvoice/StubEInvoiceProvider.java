package com.example.invoice.infrastructure.einvoice;

import com.example.invoice.domain.model.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation for development/testing.
 * Logs the action and returns a mock reference.
 *
 * <p>Active when {@code einvoice.provider} is not set or set to "stub".
 * Replace with real implementation (VnptEInvoiceProvider, ViettelEInvoiceProvider)
 * for production deployment.
 */
@Component
@ConditionalOnProperty(name = "einvoice.provider", havingValue = "stub", matchIfMissing = true)
@Slf4j
public class StubEInvoiceProvider implements EInvoiceProvider {

    @Override
    public String issue(Invoice invoice) {
        String ref = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[STUB] E-invoice issued: number={}, ref={}, total={}",
                invoice.getInvoiceNumber(), ref, invoice.getTotalAmount());
        return ref;
    }

    @Override
    public void voidInvoice(Invoice invoice) {
        log.info("[STUB] E-invoice voided: number={}, ref={}",
                invoice.getInvoiceNumber(), invoice.getEInvoiceRef());
    }
}
