package com.example.inventory.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.common.events.order.OrderShippedEvent;
import com.example.inventory.application.command.handler.ReleaseStockHandler;
import com.example.inventory.application.command.handler.ReduceStockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to domain events from the Order lifecycle.
 *
 * <p>Note: ORDER_PLACED is no longer handled here — stock reservation is now
 * coordinated by the Saga Orchestrator via {@link SagaCommandConsumer}.
 *
 * <p>Remaining responsibilities:
 * <ul>
 *   <li>OrderCancelled → release stock (safety net in addition to saga compensation)</li>
 *   <li>OrderShipped → permanently reduce stock from reserved</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ReleaseStockHandler releaseStockHandler;
    private final ReduceStockHandler  reduceStockHandler;

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "inventory-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Order cancelled {}, releasing stock...", event.getAggregateId());
        try {
            releaseStockHandler.handleByOrderId(event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to release stock for order {}: {}",
                    event.getAggregateId(), e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPED, groupId = "inventory-service")
    public void onOrderShipped(OrderShippedEvent event) {
        log.info("Order shipped {}, reducing stock...", event.getAggregateId());
        try {
            reduceStockHandler.handleByOrderId(event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to reduce stock for order {}: {}",
                    event.getAggregateId(), e.getMessage());
        }
    }
}
