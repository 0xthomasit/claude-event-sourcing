package com.example.review.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.order.OrderDeliveredEvent;
import com.example.review.domain.model.OrderDeliveryRecord;
import com.example.review.infrastructure.persistence.repository.JpaOrderDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Listens to OrderDeliveredEvent to enable reviews for delivered orders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final JpaOrderDeliveryRepository deliveryRepository;

    @KafkaListener(topics = KafkaTopics.ORDER_DELIVERED, groupId = "review-service")
    public void onOrderDelivered(OrderDeliveredEvent event) {
        log.info("OrderDelivered → enabling reviews for order {}", event.getAggregateId());
        try {
            // Idempotency check
            if (deliveryRepository.findByOrderId(event.getAggregateId()).isPresent()) {
                log.debug("Delivery record already exists for order {}", event.getAggregateId());
                return;
            }

            OrderDeliveryRecord record = OrderDeliveryRecord.builder()
                    .orderId(event.getAggregateId())
                    .customerId(event.getCustomerId())
                    .productIds(event.getProductIds() != null ? event.getProductIds().toString() : "[]")
                    .deliveredAt(event.getOccurredOn() != null ? event.getOccurredOn() : Instant.now())
                    .build();

            deliveryRepository.save(record);
            log.info("Delivery record saved for order {} — reviews enabled", event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to process OrderDeliveredEvent for order {}: {}",
                    event.getAggregateId(), e.getMessage(), e);
        }
    }
}
