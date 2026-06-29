package com.example.payment.application.command.handler;

import com.example.common.events.payment.RefundInitiatedEvent;
import com.example.payment.application.command.dto.InitiateRefundCommand;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import com.example.payment.infrastructure.messaging.projector.PaymentProjector;
import com.example.payment.infrastructure.messaging.publisher.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitiateRefundHandler {

    private final PaymentRepository paymentRepository;
    private final PaymentProjector  projector;
    private final KafkaEventPublisher publisher;

    @Transactional
    public void handle(InitiateRefundCommand cmd) {
        Payment payment = paymentRepository.findByOrderId(cmd.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "No payment found for orderId: " + cmd.getOrderId()));

        payment.initiateRefund(cmd.getReason());

        var events = payment.getUncommittedEvents();

        // 1. Persist to Event Store FIRST
        paymentRepository.save(payment);

        // 2. Update Read Model and publish Kafka
        projector.projectAll(events);
        publisher.publishAll(payment.getId().toString(), events);

        log.info("Refund initiated: paymentId={}, orderId={}",
                payment.getId(), cmd.getOrderId());
    }
}
