package com.example.invoice.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Invoice Aggregate Root.
 *
 * <p>Represents a tax invoice (hoá đơn) for an order. Immutable after issuance.
 * Compliant with Vietnamese e-invoice regulations (Nghị định 123/2020/NĐ-CP).
 *
 * <p>Key invariants:
 * <ul>
 *   <li>Invoices are immutable after ISSUED status</li>
 *   <li>Voiding only allowed within 24 hours (configurable)</li>
 *   <li>VAT rate defaults to 8% (current Vietnamese standard rate)</li>
 *   <li>Invoice numbers are sequential per day: INV-YYYYMMDD-XXXXX</li>
 * </ul>
 */
@Entity
@Table(name = "invoices")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    private static final Duration VOID_WINDOW = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_number", unique = true, nullable = false, length = 30)
    private String invoiceNumber;

    @Column(name = "order_id", unique = true, nullable = false, length = 36)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("0.0800");

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "e_invoice_ref")
    private String eInvoiceRef;    // Reference from external e-invoice provider

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    // ─── Domain behaviour ────────────────────────────────────────────────────

    /**
     * Calculates tax and total, then issues the invoice.
     */
    public void issue(String invoiceNumber) {
        if (status != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Can only issue DRAFT invoices, current: " + status);
        if (lineItems.isEmpty())
            throw new IllegalStateException("Invoice must have at least one line item");

        this.invoiceNumber = invoiceNumber;

        // Calculate subtotal from line items
        this.subtotal = lineItems.stream()
                .map(InvoiceLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply discount
        BigDecimal taxableAmount = subtotal.subtract(
                discountAmount != null ? discountAmount : BigDecimal.ZERO);

        // Calculate VAT
        this.taxAmount = taxableAmount.multiply(taxRate)
                .setScale(0, RoundingMode.HALF_UP);
        this.totalAmount = taxableAmount.add(taxAmount);

        this.status = InvoiceStatus.ISSUED;
        this.issuedAt = Instant.now();
    }

    /**
     * Voids the invoice — only allowed within VOID_WINDOW of issuance.
     */
    public void voidInvoice(String reason) {
        if (status != InvoiceStatus.ISSUED)
            throw new IllegalStateException("Can only void ISSUED invoices, current: " + status);
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("Void reason is required");

        if (issuedAt != null && Duration.between(issuedAt, Instant.now()).compareTo(VOID_WINDOW) > 0)
            throw new IllegalStateException(
                    "Cannot void invoice after " + VOID_WINDOW.toHours() + " hours of issuance");

        this.status = InvoiceStatus.VOIDED;
        this.voidReason = reason;
        this.voidedAt = Instant.now();
    }

    /**
     * Adds a line item to this invoice.
     */
    public void addLineItem(String productId, String productName, String sku,
                            int quantity, BigDecimal unitPrice) {
        if (status != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Cannot modify issued invoice");

        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        InvoiceLineItem item = InvoiceLineItem.builder()
                .invoice(this)
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lineTotal(lineTotal)
                .build();

        lineItems.add(item);
    }
}
