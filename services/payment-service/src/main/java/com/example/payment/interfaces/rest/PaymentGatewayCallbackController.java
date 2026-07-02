package com.example.payment.interfaces.rest;

import com.example.common.events.saga.PaymentProcessedReply;
import com.example.common.events.saga.SagaTopics;
import com.example.payment.application.command.dto.CompletePaymentCommand;
import com.example.payment.application.command.dto.FailPaymentCommand;
import com.example.payment.application.command.handler.CompletePaymentHandler;
import com.example.payment.application.command.handler.FailPaymentHandler;
import com.example.payment.infrastructure.persistence.entity.SagaPaymentCorrelation;
import com.example.payment.infrastructure.persistence.repository.SagaPaymentCorrelationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simulates an external payment gateway callback.
 *
 * <p>In production, this endpoint would be called by the payment gateway
 * (e.g., Stripe webhook, VNPay IPN) after processing the payment.
 * The callback completes/fails the payment and sends the saga reply.
 *
 * <p>Flow:
 * <pre>
 * Gateway → POST /api/payments/{paymentId}/gateway-callback
 *         → Complete/Fail payment
 *         → Send PaymentProcessedReply to saga orchestrator
 * </pre>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Gateway", description = "Async payment gateway callback endpoint")
public class PaymentGatewayCallbackController {

    private final CompletePaymentHandler completePaymentHandler;
    private final FailPaymentHandler failPaymentHandler;
    private final SagaPaymentCorrelationRepository correlationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/{paymentId}/gateway-callback")
    @Operation(summary = "Payment gateway async callback — completes or fails a payment and notifies the saga orchestrator")
    public ResponseEntity<Map<String, String>> gatewayCallback(
            @PathVariable String paymentId,
            @Valid @RequestBody GatewayCallbackRequest request) {

        log.info("Gateway callback received: paymentId={}, status={}", paymentId, request.getStatus());

        // 1. Find saga correlation
        SagaPaymentCorrelation correlation = correlationRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No saga correlation found for paymentId: " + paymentId));

        boolean success;
        String failureReason = null;

        try {
            if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
                // 2a. Complete payment
                completePaymentHandler.handle(CompletePaymentCommand.builder()
                        .paymentId(paymentId)
                        .transactionRef(request.getTransactionRef())
                        .build());
                success = true;
                log.info("Payment completed via gateway callback: paymentId={}", paymentId);
            } else {
                // 2b. Fail payment
                failPaymentHandler.handle(FailPaymentCommand.builder()
                        .paymentId(paymentId)
                        .reason(request.getReason())
                        .build());
                success = false;
                failureReason = request.getReason();
                log.info("Payment failed via gateway callback: paymentId={}, reason={}", paymentId, failureReason);
            }
        } catch (Exception e) {
            success = false;
            failureReason = e.getMessage();
            log.error("Gateway callback processing error: paymentId={}, error={}", paymentId, e.getMessage());
        }

        // 3. Send reply back to saga orchestrator
        PaymentProcessedReply reply = PaymentProcessedReply.builder()
                .sagaId(correlation.getSagaId())
                .orderId(correlation.getOrderId())
                .success(success)
                .paymentId(paymentId)
                .failureReason(failureReason)
                .build();

        kafkaTemplate.send(SagaTopics.PROCESS_PAYMENT_REPLY, correlation.getOrderId(), reply);
        log.info("Sent PaymentProcessedReply to saga {}: success={}", correlation.getSagaId(), success);

        return ResponseEntity.ok(Map.of(
                "paymentId", paymentId,
                "sagaId", correlation.getSagaId(),
                "status", success ? "COMPLETED" : "FAILED"));
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GatewayCallbackRequest {
        @NotBlank
        private String status;          // "SUCCESS" or "FAILED"
        private String transactionRef;  // gateway transaction reference (required for SUCCESS)
        private String reason;          // failure reason (required for FAILED)
    }
}
