package com.example.payment.application.command.handler;

import com.example.common.domain.exception.AggregateNotFoundException;
import com.example.common.events.payment.PaymentCompletedEvent;
import com.example.payment.application.command.dto.CompletePaymentCommand;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import com.example.payment.infrastructure.messaging.projector.PaymentProjector;
import com.example.payment.infrastructure.messaging.publisher.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompletePaymentHandler {

    private final PaymentRepository   paymentRepository;
    private final PaymentProjector    projector;
    private final KafkaEventPublisher publisher;

    @Transactional
    public void handle(CompletePaymentCommand cmd) {
        Payment payment = paymentRepository.findById(UUID.fromString(cmd.getPaymentId()))
                .orElseThrow(() -> new AggregateNotFoundException(
                        "Payment not found: " + cmd.getPaymentId()));

        // Start processing then complete (simulate gateway callback)
        payment.startProcessing();
        payment.complete(cmd.getTransactionRef());

        // Capture events BEFORE save clears them
        var events = payment.getUncommittedEvents();

        // 1. Persist to Event Store FIRST
        paymentRepository.save(payment);

        // 2. Update Read Model
        events.stream()
                .filter(e -> e instanceof PaymentCompletedEvent)
                .map(e -> (PaymentCompletedEvent) e)
                .forEach(e -> {
                    projector.on(e);
                    // 3. Publish to Kafka AFTER Event Store persisted
                    publisher.publish(e);
                });

        log.info("Payment completed: paymentId={}, orderId={}",
                cmd.getPaymentId(), payment.getOrderId());
    }
}
