package com.example.payment.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.payment.application.command.dto.InitiateRefundCommand;
import com.example.payment.application.command.handler.InitiateRefundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to domain events from the Order lifecycle.
 *
 * <p>Note: ORDER_PLACED is no longer handled here — payment processing is now
 * coordinated by the Saga Orchestrator via {@link SagaCommandConsumer}.
 *
 * <p>Remaining responsibilities:
 * <ul>
 *   <li>OrderCancelled → initiate refund if payment was completed</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InitiateRefundHandler initiateRefundHandler;

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
