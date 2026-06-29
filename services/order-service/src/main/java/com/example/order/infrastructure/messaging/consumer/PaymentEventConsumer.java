package com.example.order.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.payment.PaymentCompletedEvent;
import com.example.common.events.payment.PaymentFailedEvent;
import com.example.order.application.command.dto.CancelOrderCommand;
import com.example.order.application.command.dto.ConfirmOrderCommand;
import com.example.order.application.command.handler.CancelOrderHandler;
import com.example.order.application.command.handler.ConfirmOrderHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Saga choreography: listens to payment events → drives order state transitions.
 * PaymentCompleted → ConfirmOrder
 * PaymentFailed    → CancelOrder
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ConfirmOrderHandler confirmOrderHandler;
    private final CancelOrderHandler  cancelOrderHandler;

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "order-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed for order {}, confirming...", event.getOrderId());
        try {
            confirmOrderHandler.handle(
                    new ConfirmOrderCommand(UUID.fromString(event.getOrderId()), "system"));
        } catch (Exception e) {
            log.error("Failed to confirm order {} after payment: {}", event.getOrderId(), e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "order-service")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Payment failed for order {}, cancelling...", event.getOrderId());
        try {
            cancelOrderHandler.handle(new CancelOrderCommand(
                    UUID.fromString(event.getOrderId()),
                    "Payment failed: " + event.getFailureReason(),
                    "system"));
        } catch (Exception e) {
            log.error("Failed to cancel order {} after payment failure: {}", event.getOrderId(), e.getMessage());
        }
    }
}