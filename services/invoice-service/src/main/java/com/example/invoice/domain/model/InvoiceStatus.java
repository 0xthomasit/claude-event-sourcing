package com.example.invoice.domain.model;

public enum InvoiceStatus {
    DRAFT,    // Created but not finalized
    ISSUED,   // Finalized and sent to e-invoice provider
    VOIDED    // Cancelled within legal timeframe
}
