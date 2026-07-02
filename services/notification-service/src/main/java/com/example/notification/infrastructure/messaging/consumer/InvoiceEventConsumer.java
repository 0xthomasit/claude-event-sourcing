package com.example.notification.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.invoice.InvoiceIssuedEvent;
import com.example.notification.application.service.NotificationService;
import com.example.notification.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes invoice events and sends notifications.
 * Sends invoice email with download link when an invoice is issued.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.INVOICE_ISSUED, groupId = "notification-service")
    public void onInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Notification: InvoiceIssued invoice={}, order={}", 
                event.getInvoiceNumber(), event.getOrderId());
        try {
            String email = event.getCustomerEmail() != null
                    ? event.getCustomerEmail()
                    : event.getCustomerId() + "@example.com";   // fallback

            notificationService.sendEmail(
                    event.getCustomerId(),
                    event.getOrderId(),
                    NotificationType.INVOICE_ISSUED,
                    email,
                    "Hoá đơn #" + event.getInvoiceNumber() + " cho đơn hàng #" + event.getOrderId(),
                    buildInvoiceBody(event)
            );
        } catch (Exception e) {
            log.error("Failed to send invoice notification for {}: {}",
                    event.getInvoiceNumber(), e.getMessage(), e);
        }
    }

    private String buildInvoiceBody(InvoiceIssuedEvent event) {
        return String.format(
                "Hoá đơn điện tử đã được xuất cho đơn hàng của bạn.\n\n" +
                "Số hoá đơn: %s\n" +
                "Đơn hàng: %s\n" +
                "Tổng tiền: %s %s\n\n" +
                "Bạn có thể xem và tải hoá đơn tại trang chi tiết đơn hàng.",
                event.getInvoiceNumber(),
                event.getOrderId(),
                event.getTotalAmount(),
                event.getCurrency() != null ? event.getCurrency() : "VND"
        );
    }
}
