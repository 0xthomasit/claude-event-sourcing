package com.example.invoice.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.common.events.order.OrderConfirmedEvent;
import com.example.invoice.application.command.handler.IssueInvoiceHandler;
import com.example.invoice.application.command.handler.VoidInvoiceHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to order lifecycle events:
 * <ul>
 *   <li>OrderConfirmed → auto-issue invoice</li>
 *   <li>OrderCancelled → auto-void invoice</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final IssueInvoiceHandler issueHandler;
    private final VoidInvoiceHandler  voidHandler;

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED, groupId = "invoice-service")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("OrderConfirmed → issuing invoice for order {}", event.getAggregateId());
        try {
            issueHandler.handle(event);
        } catch (Exception e) {
            log.error("Failed to issue invoice for order {}: {}",
                    event.getAggregateId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "invoice-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("OrderCancelled → voiding invoice for order {}", event.getAggregateId());
        try {
            voidHandler.handleByOrderId(event.getAggregateId(), "Order cancelled");
        } catch (Exception e) {
            log.error("Failed to void invoice for order {}: {}",
                    event.getAggregateId(), e.getMessage(), e);
        }
    }
}
