package com.example.payment.interfaces.rest;

import com.example.payment.application.command.dto.CompletePaymentCommand;
import com.example.payment.application.command.dto.FailPaymentCommand;
import com.example.payment.application.command.dto.InitiatePaymentCommand;
import com.example.payment.application.command.dto.InitiateRefundCommand;
import com.example.payment.application.command.handler.CompletePaymentHandler;
import com.example.payment.application.command.handler.FailPaymentHandler;
import com.example.payment.application.command.handler.InitiatePaymentHandler;
import com.example.payment.application.command.handler.InitiateRefundHandler;
import com.example.payment.application.query.dto.PaymentResponse;
import com.example.payment.application.query.handler.PaymentQueryHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management — CQRS + Event Sourcing")
public class PaymentController {

    private final InitiatePaymentHandler initiatePaymentHandler;
    private final CompletePaymentHandler completePaymentHandler;
    private final FailPaymentHandler     failPaymentHandler;
    private final InitiateRefundHandler  initiateRefundHandler;
    private final PaymentQueryHandler    queryHandler;

    // ─── Commands ─────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Initiate a payment for an order")
    public ResponseEntity<Map<String, String>> initiatePayment(
            @Valid @RequestBody InitiatePaymentCommand cmd) {
        UUID paymentId = initiatePaymentHandler.handle(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("paymentId", paymentId.toString()));
    }

    @PostMapping("/{paymentId}/complete")
    @Operation(summary = "Complete a payment (gateway callback)")
    public ResponseEntity<Void> completePayment(
            @PathVariable String paymentId,
            @RequestParam String transactionRef) {
        completePaymentHandler.handle(CompletePaymentCommand.builder()
                .paymentId(paymentId)
                .transactionRef(transactionRef)
                .build());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{paymentId}/fail")
    @Operation(summary = "Fail a payment (gateway callback)")
    public ResponseEntity<Void> failPayment(
            @PathVariable String paymentId,
            @RequestParam String reason) {
        failPaymentHandler.handle(FailPaymentCommand.builder()
                .paymentId(paymentId)
                .reason(reason)
                .build());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund")
    @Operation(summary = "Initiate a refund for an order")
    public ResponseEntity<Void> initiateRefund(
            @Valid @RequestBody InitiateRefundCommand cmd) {
        initiateRefundHandler.handle(cmd);
        return ResponseEntity.ok().build();
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getById(@PathVariable String paymentId) {
        return queryHandler.findById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable String orderId) {
        return queryHandler.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all payments for a customer")
    public ResponseEntity<List<PaymentResponse>> getByCustomer(
            @RequestParam String customerId) {
        return ResponseEntity.ok(queryHandler.findByCustomerId(customerId));
    }
}
