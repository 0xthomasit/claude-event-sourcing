package com.example.invoice.infrastructure.persistence.repository;

import com.example.invoice.domain.model.Invoice;
import com.example.invoice.domain.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaInvoiceRepository extends JpaRepository<Invoice, UUID>, InvoiceRepository {
    @Override
    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByOrderId(String orderId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Page<Invoice> findByCustomerId(String customerId, Pageable pageable);
    long countByInvoiceNumberStartingWith(String prefix);
}
