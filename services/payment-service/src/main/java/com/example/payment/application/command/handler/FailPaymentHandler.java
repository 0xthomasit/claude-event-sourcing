package com.example.payment.application.command.handler;

import com.example.common.domain.exception.AggregateNotFoundException;
import com.example.common.events.payment.PaymentFailedEvent;
import com.example.payment.application.command.dto.FailPaymentCommand;
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
public class FailPaymentHandler {

    private final PaymentRepository   paymentRepository;
    private final PaymentProjector    projector;
    private final KafkaEventPublisher publisher;

    @Transactional
    public void handle(FailPaymentCommand cmd) {
        Payment payment = paymentRepository.findById(UUID.fromString(cmd.getPaymentId()))
                .orElseThrow(() -> new AggregateNotFoundException(
                        "Payment not found: " + cmd.getPaymentId()));

        payment.fail(cmd.getReason());

        var events = payment.getUncommittedEvents();

        // 1. Persist to Event Store FIRST
        paymentRepository.save(payment);

        // 2. Update Read Model + publish Kafka (Saga rollback trigger)
        projector.projectAll(events);
        publisher.publishAll(payment.getId().toString(), events);

        log.info("Payment failed: paymentId={}, orderId={}, reason={}",
                cmd.getPaymentId(), payment.getOrderId(), cmd.getReason());
    }
}
