package com.example.notification.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.payment.PaymentCompletedEvent;
import com.example.common.events.payment.PaymentFailedEvent;
import com.example.notification.application.service.NotificationService;
import com.example.notification.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment domain events and triggers notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "notification-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Notification: PaymentCompleted orderId={}", event.getOrderId());
        notificationService.sendEmail(
                event.getAggregateId(),
                event.getOrderId(),
                NotificationType.PAYMENT_COMPLETED,
                "customer@example.com",   // real impl: lookup from user-service
                "Thanh toán đơn hàng #" + event.getOrderId() + " thành công",
                String.format("Thanh toán %s VND cho đơn hàng #%s đã được xác nhận.",
                        event.getAmount(), event.getOrderId())
        );
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "notification-service")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Notification: PaymentFailed orderId={}", event.getOrderId());
        notificationService.sendEmail(
                event.getAggregateId(),
                event.getOrderId(),
                NotificationType.PAYMENT_FAILED,
                "customer@example.com",
                "Thanh toán đơn hàng #" + event.getOrderId() + " thất bại",
                "Thanh toán của bạn không thành công. Lý do: " + event.getFailureReason()
        );
    }
}
