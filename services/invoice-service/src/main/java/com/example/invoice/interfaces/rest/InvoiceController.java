package com.example.invoice.interfaces.rest;

import com.example.invoice.application.command.handler.VoidInvoiceHandler;
import com.example.invoice.application.query.dto.InvoiceResponse;
import com.example.invoice.application.query.handler.InvoiceQueryHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "E-invoice management and lookup")
public class InvoiceController {

    private final InvoiceQueryHandler queryHandler;
    private final VoidInvoiceHandler  voidHandler;

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(queryHandler.findById(id));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get invoice by order ID")
    public ResponseEntity<InvoiceResponse> getByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(queryHandler.findByOrderId(orderId));
    }

    @GetMapping("/number/{number}")
    @Operation(summary = "Get invoice by invoice number")
    public ResponseEntity<InvoiceResponse> getByNumber(@PathVariable String number) {
        return ResponseEntity.ok(queryHandler.findByInvoiceNumber(number));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List invoices for a customer")
    public ResponseEntity<Page<InvoiceResponse>> getByCustomer(
            @PathVariable String customerId, Pageable pageable) {
        return ResponseEntity.ok(queryHandler.findByCustomerId(customerId, pageable));
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "Void an invoice (ADMIN)")
    public ResponseEntity<InvoiceResponse> voidInvoice(
            @PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(InvoiceResponse.from(voidHandler.handle(id, reason)));
    }
}
