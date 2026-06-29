package com.example.payment.application.command.handler;

import com.example.common.domain.model.Money;
import com.example.common.events.payment.PaymentInitiatedEvent;
import com.example.payment.application.command.dto.InitiatePaymentCommand;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentMethod;
import com.example.payment.domain.model.PaymentMethodType;
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
public class InitiatePaymentHandler {

    private final PaymentRepository paymentRepository;
    private final PaymentProjector  projector;
    private final KafkaEventPublisher publisher;

    @Transactional
    public UUID handle(InitiatePaymentCommand cmd) {
        // Idempotency — prevent duplicate payment for same order
        paymentRepository.findByOrderId(cmd.getOrderId()).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Payment already exists for orderId: " + cmd.getOrderId());
        });

        Money money = Money.of(cmd.getAmount(), cmd.getCurrency());
        PaymentMethod method = PaymentMethod.of(
                PaymentMethodType.valueOf(cmd.getPaymentMethodType()),
                cmd.getMaskedAccount());

        Payment payment = Payment.initiate(
                cmd.getOrderId(), cmd.getCustomerId(), money, method);

        // Capture events BEFORE save clears them
        var events = payment.getUncommittedEvents();

        // 1. Persist to Event Store (source of truth — FIRST)
        paymentRepository.save(payment);

        // 2. Update Read Model synchronously and publish to Kafka
        projector.projectAll(events);
        publisher.publishAll(payment.getId().toString(), events);

        log.info("Payment initiated: paymentId={}, orderId={}",
                payment.getId(), cmd.getOrderId());

        return payment.getId();
    }
}