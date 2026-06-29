package com.example.payment.infrastructure.messaging.consumer;

import com.example.common.domain.model.Money;
import com.example.common.events.KafkaTopics;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.common.events.order.OrderPlacedEvent;
import com.example.payment.application.command.dto.InitiatePaymentCommand;
import com.example.payment.application.command.dto.InitiateRefundCommand;
import com.example.payment.application.command.handler.InitiatePaymentHandler;
import com.example.payment.application.command.handler.InitiateRefundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order events and triggers payment commands.
 *
 * Saga pattern:
 *   OrderPlacedEvent    → InitiatePaymentCommand
 *   OrderCancelledEvent → InitiateRefundCommand (if payment was completed)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InitiatePaymentHandler initiatePaymentHandler;
    private final InitiateRefundHandler  initiateRefundHandler;

    @KafkaListener(topics = KafkaTopics.ORDER_PLACED, groupId = "payment-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent: orderId={}, customerId={}",
                event.getAggregateId(), event.getCustomerId());
        try {
            InitiatePaymentCommand command = InitiatePaymentCommand.builder()
                    .orderId(event.getAggregateId())
                    .customerId(event.getCustomerId())
                    .amount(event.getTotalAmount())
                    .currency("VND")
                    .paymentMethodType("BANKING")   // default; real impl reads from order
                    .maskedAccount(null)
                    .build();
            initiatePaymentHandler.handle(command);
        } catch (Exception e) {
            log.error("Failed to initiate payment for orderId={}: {}",
                    event.getAggregateId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "payment-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}", event.getAggregateId());
        try {
            InitiateRefundCommand command = InitiateRefundCommand.builder()
                    .orderId(event.getAggregateId())
                    .reason(event.getReason())
                    .build();
            initiateRefundHandler.handle(command);
        } catch (Exception e) {
            log.warn("Could not initiate refund for orderId={} (may not have been paid): {}",
                    event.getAggregateId(), e.getMessage());
        }
    }
}
