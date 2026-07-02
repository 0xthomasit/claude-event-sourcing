package com.example.payment.infrastructure.messaging.consumer;

import com.example.common.events.saga.PaymentProcessedReply;
import com.example.common.events.saga.ProcessPaymentCommand;
import com.example.common.events.saga.SagaTopics;
import com.example.payment.application.command.dto.InitiatePaymentCommand;
import com.example.payment.application.command.handler.InitiatePaymentHandler;
import com.example.payment.infrastructure.persistence.entity.SagaPaymentCorrelation;
import com.example.payment.infrastructure.persistence.repository.SagaPaymentCorrelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles saga commands from the Order Saga Orchestrator.
 *
 * <p>Receives ProcessPayment command and initiates payment, but does NOT
 * complete it synchronously. Instead, it stores a saga-payment correlation
 * and waits for an async gateway callback (REST endpoint) to complete/fail
 * the payment and send the reply back to the orchestrator.
 *
 * <p>Flow:
 * <pre>
 * 1. Orchestrator → ProcessPaymentCommand → this consumer
 * 2. Consumer → initiate payment → store correlation (paymentId ↔ sagaId)
 * 3. [async] Gateway → POST /api/payments/{id}/gateway-callback → complete/fail
 * 4. Gateway callback handler → sends PaymentProcessedReply to orchestrator
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCommandConsumer {

    private final InitiatePaymentHandler initiatePaymentHandler;
    private final SagaPaymentCorrelationRepository correlationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = SagaTopics.PROCESS_PAYMENT_CMD, groupId = "payment-saga")
    public void onProcessPayment(ProcessPaymentCommand cmd) {
        log.info("Saga {} → ProcessPayment for order {}, amount={}",
                cmd.getSagaId(), cmd.getOrderId(), cmd.getAmount());

        try {
            // Step 1: Initiate payment (sets status to INITIATED)
            InitiatePaymentCommand initiateCmd = InitiatePaymentCommand.builder()
                    .orderId(cmd.getOrderId())
                    .customerId(cmd.getCustomerId())
                    .amount(cmd.getAmount())
                    .currency(cmd.getCurrency())
                    .paymentMethodType("BANKING")
                    .maskedAccount(null)
                    .build();
            UUID paymentId = initiatePaymentHandler.handle(initiateCmd);

            // Step 2: Store correlation for async gateway callback
            SagaPaymentCorrelation correlation = SagaPaymentCorrelation.builder()
                    .paymentId(paymentId.toString())
                    .sagaId(cmd.getSagaId())
                    .orderId(cmd.getOrderId())
                    .createdAt(Instant.now())
                    .build();
            correlationRepository.save(correlation);

            log.info("Payment initiated: paymentId={}, awaiting gateway callback. " +
                    "Correlation stored: sagaId={}", paymentId, cmd.getSagaId());

            // Reply will be sent when gateway calls POST /api/payments/{id}/gateway-callback

        } catch (Exception e) {
            log.error("Payment initiation failed for order {}: {}", cmd.getOrderId(), e.getMessage());

            // Immediate failure → reply with failure so saga can compensate
            PaymentProcessedReply reply = PaymentProcessedReply.builder()
                    .sagaId(cmd.getSagaId())
                    .orderId(cmd.getOrderId())
                    .success(false)
                    .failureReason(e.getMessage())
                    .build();
            kafkaTemplate.send(SagaTopics.PROCESS_PAYMENT_REPLY, cmd.getOrderId(), reply);
        }
    }
}
