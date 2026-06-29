package com.example.shipping.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.order.OrderConfirmedEvent;
import com.example.shipping.application.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes OrderConfirmedEvent → creates a shipment record.
 * Real impl would call GHN/GHTK API to create shipping order.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ShippingService shippingService;

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED, groupId = "shipping-service")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received OrderConfirmedEvent: orderId={}", event.getAggregateId());
        try {
            shippingService.createShipment(
                    event.getAggregateId(),
                    event.getCustomerId(),
                    event.getCustomerId(),   // real impl: lookup from user-service
                    "0900000000",            // real impl: from order shipping address
                    "123 Nguyễn Huệ",        // real impl: from order shipping address
                    "Quận 1",
                    "TP. Hồ Chí Minh"
            );
        } catch (IllegalStateException e) {
            log.warn("Shipment already exists for orderId={}: {}", event.getAggregateId(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create shipment for orderId={}: {}",
                    event.getAggregateId(), e.getMessage(), e);
        }
    }
}
