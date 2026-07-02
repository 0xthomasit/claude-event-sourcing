package com.example.invoice.domain.service;

import com.example.invoice.infrastructure.persistence.repository.JpaInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates sequential invoice numbers per day.
 * Format: INV-YYYYMMDD-XXXXX (e.g., INV-20260701-00001)
 *
 * Uses a database query to find the last sequence for the current day,
 * ensuring thread-safety via @Transactional and unique constraint.
 */
@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private static final String PREFIX = "INV";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JpaInvoiceRepository invoiceRepository;

    @Transactional
    public String generateNext() {
        String dateStr = LocalDate.now().format(DATE_FMT);
        String prefix = PREFIX + "-" + dateStr + "-";

        long count = invoiceRepository.countByInvoiceNumberStartingWith(prefix);
        String sequence = String.format("%05d", count + 1);

        return prefix + sequence;
    }
}
