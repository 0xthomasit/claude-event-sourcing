package com.example.invoice.application.query.handler;

import com.example.invoice.application.query.dto.InvoiceResponse;
import com.example.invoice.infrastructure.persistence.repository.JpaInvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceQueryHandler {

    private final JpaInvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public InvoiceResponse findById(UUID id) {
        return invoiceRepository.findById(id)
                .map(InvoiceResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findByOrderId(String orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .map(InvoiceResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found for order: " + orderId));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findByInvoiceNumber(String number) {
        return invoiceRepository.findByInvoiceNumber(number)
                .map(InvoiceResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + number));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findByCustomerId(String customerId, Pageable pageable) {
        return invoiceRepository.findByCustomerId(customerId, pageable)
                .map(InvoiceResponse::from);
    }
}
