package com.example.inventory.infrastructure.messaging.consumer;

import com.example.common.events.KafkaTopics;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.common.events.order.OrderPlacedEvent;
import com.example.common.events.order.OrderShippedEvent;
import com.example.inventory.application.command.dto.ReleaseStockCommand;
import com.example.inventory.application.command.dto.ReduceStockCommand;
import com.example.inventory.application.command.dto.ReserveStockCommand;
import com.example.inventory.application.command.handler.ReleaseStockHandler;
import com.example.inventory.application.command.handler.ReduceStockHandler;
import com.example.inventory.application.command.handler.ReserveStockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Saga choreography: reacts to order lifecycle events.
 *
 * OrderPlaced   → ReserveStock  (lock stock for the order)
 * OrderCancelled → ReleaseStock (unlock stock back to available)
 * OrderShipped  → ReduceStock   (permanently deduct from inventory)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ReserveStockHandler reserveStockHandler;
    private final ReleaseStockHandler releaseStockHandler;
    private final ReduceStockHandler  reduceStockHandler;

    @KafkaListener(topics = KafkaTopics.ORDER_PLACED, groupId = "inventory-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order placed {}, reserving stock for {} items...",
                event.getAggregateId(), event.getItems().size());
        boolean failed = false;
        for (var item : event.getItems()) {
            try {
                reserveStockHandler.handle(ReserveStockCommand.builder()
                        .productId(item.getProductId())
                        .orderId(event.getAggregateId())
                        .quantity(item.getQuantity())
                        .build());
            } catch (Exception e) {
                log.error("Failed to reserve stock for product {} order {}: {}",
                        item.getProductId(), event.getAggregateId(), e.getMessage());
                failed = true;
                break;
            }
        }

        if (failed) {
            log.info("Rolling back partial stock reservations for order {}", event.getAggregateId());
            try {
                releaseStockHandler.handleByOrderId(event.getAggregateId());
            } catch (Exception ex) {
                log.error("Failed to rollback reservations for order {}: {}", event.getAggregateId(), ex.getMessage());
            }
            // TODO: publish StockReservationFailedEvent → trigger order cancellation saga
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "inventory-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Order cancelled {}, releasing stock...", event.getAggregateId());
        // TODO: in production, track which items were reserved per order
        // For now, the handler looks up by orderId
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