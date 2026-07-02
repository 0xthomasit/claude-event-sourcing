package com.example.invoice.application.query.dto;

import com.example.invoice.domain.model.Invoice;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceResponse {
    private UUID id;
    private String invoiceNumber;
    private String orderId;
    private String customerId;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String eInvoiceRef;
    private String voidReason;
    private Instant issuedAt;
    private Instant voidedAt;
    private List<LineItemResponse> lineItems;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineItemResponse {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    public static InvoiceResponse from(Invoice inv) {
        return InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .orderId(inv.getOrderId())
                .customerId(inv.getCustomerId())
                .subtotal(inv.getSubtotal())
                .discountAmount(inv.getDiscountAmount())
                .taxRate(inv.getTaxRate())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .currency(inv.getCurrency())
                .status(inv.getStatus().name())
                .eInvoiceRef(inv.getEInvoiceRef())
                .voidReason(inv.getVoidReason())
                .issuedAt(inv.getIssuedAt())
                .voidedAt(inv.getVoidedAt())
                .lineItems(inv.getLineItems().stream().map(li ->
                        LineItemResponse.builder()
                                .productId(li.getProductId())
                                .productName(li.getProductName())
                                .quantity(li.getQuantity())
                                .unitPrice(li.getUnitPrice())
                                .lineTotal(li.getLineTotal())
                                .build()
                ).toList())
                .build();
    }
}
