package com.example.invoice.domain.repository;

import com.example.invoice.domain.model.Invoice;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(UUID id);
    Optional<Invoice> findByOrderId(String orderId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
