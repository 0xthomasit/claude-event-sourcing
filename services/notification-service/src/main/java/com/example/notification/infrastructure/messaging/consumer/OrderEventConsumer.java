package com.example.notification.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.order.*;
import com.example.notification.application.service.NotificationService;
import com.example.notification.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order domain events and triggers email/SMS notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.ORDER_PLACED, groupId = "notification-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Notification: OrderPlaced orderId={}", event.getAggregateId());
        notificationService.sendEmail(
                event.getCustomerId(),
                event.getAggregateId(),
                NotificationType.ORDER_PLACED,
                event.getCustomerId() + "@example.com",   // real impl: lookup from user-service
                "Đơn hàng #" + event.getAggregateId() + " đã được đặt thành công",
                buildOrderPlacedBody(event)
        );
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED, groupId = "notification-service")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Notification: OrderConfirmed orderId={}", event.getAggregateId());
        notificationService.sendEmail(
                event.getCustomerId(),
                event.getAggregateId(),
                NotificationType.ORDER_CONFIRMED,
                event.getCustomerId() + "@example.com",
                "Đơn hàng #" + event.getAggregateId() + " đã được xác nhận",
                "Đơn hàng của bạn đã được xác nhận và đang được xử lý."
        );
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "notification-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Notification: OrderCancelled orderId={}", event.getAggregateId());
        notificationService.sendEmail(
                event.getCustomerId(),
                event.getAggregateId(),
                NotificationType.ORDER_CANCELLED,
                event.getCustomerId() + "@example.com",
                "Đơn hàng #" + event.getAggregateId() + " đã bị hủy",
                "Đơn hàng của bạn đã bị hủy. Lý do: " + event.getReason()
        );
    }

    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPED, groupId = "notification-service")
    public void onOrderShipped(OrderShippedEvent event) {
        log.info("Notification: OrderShipped orderId={}", event.getAggregateId());
        notificationService.sendEmail(
                event.getCustomerId(),
                event.getAggregateId(),
                NotificationType.ORDER_SHIPPED,
                event.getCustomerId() + "@example.com",
                "Đơn hàng #" + event.getAggregateId() + " đang được giao",
                "Đơn hàng của bạn đã được giao cho đơn vị vận chuyển. Mã vận đơn: "
                        + event.getTrackingNumber()
        );
    }

    private String buildOrderPlacedBody(OrderPlacedEvent event) {
        return String.format(
                "Cảm ơn bạn đã đặt hàng!\n\nMã đơn hàng: %s\nTổng tiền: %s VND\n\nChúng tôi sẽ xử lý đơn hàng của bạn sớm nhất.",
                event.getAggregateId(),
                event.getTotalAmount()
        );
    }
}
