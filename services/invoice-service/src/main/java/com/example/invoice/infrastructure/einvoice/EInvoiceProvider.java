package com.example.invoice.infrastructure.einvoice;

import com.example.invoice.domain.model.Invoice;

/**
 * Port to external e-invoice provider (VNPT, Viettel, MISA, etc.).
 *
 * <p>In production, implement this interface to call the provider's API.
 * The default stub logs and returns a mock reference.
 */
public interface EInvoiceProvider {

    /**
     * Issues an invoice with the external provider.
     *
     * @param invoice the invoice to issue
     * @return external reference ID from the provider
     */
    String issue(Invoice invoice);

    /**
     * Voids a previously issued invoice.
     */
    void voidInvoice(Invoice invoice);
}
