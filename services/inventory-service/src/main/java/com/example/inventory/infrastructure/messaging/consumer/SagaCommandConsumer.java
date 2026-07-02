package com.example.inventory.infrastructure.messaging.consumer;

import com.example.common.events.saga.*;
import com.example.inventory.application.command.dto.ReserveStockCommand;
import com.example.inventory.application.command.handler.ReleaseStockHandler;
import com.example.inventory.application.command.handler.ReserveStockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Handles saga commands from the Order Saga Orchestrator.
 *
 * <p>Receives reserve/release stock commands, executes the business logic,
 * and sends a reply back to the orchestrator indicating success or failure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCommandConsumer {

    private final ReserveStockHandler reserveStockHandler;
    private final ReleaseStockHandler releaseStockHandler;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = SagaTopics.RESERVE_STOCK_CMD, groupId = "inventory-saga")
    public void onReserveStock(com.example.common.events.saga.ReserveStockCommand cmd) {
        log.info("Saga {} → ReserveStock for order {}, {} items",
                cmd.getSagaId(), cmd.getOrderId(), cmd.getItems().size());

        boolean allReserved = true;
        String failureReason = null;

        for (var item : cmd.getItems()) {
            try {
                reserveStockHandler.handle(ReserveStockCommand.builder()
                        .productId(item.getProductId())
                        .orderId(cmd.getOrderId())
                        .quantity(item.getQuantity())
                        .build());
            } catch (Exception e) {
                log.error("Failed to reserve stock for product {} order {}: {}",
                        item.getProductId(), cmd.getOrderId(), e.getMessage());
                allReserved = false;
                failureReason = e.getMessage();
                break;
            }
        }

        if (!allReserved) {
            // Rollback partial reservations
            log.info("Rolling back partial reservations for order {}", cmd.getOrderId());
            try {
                releaseStockHandler.handleByOrderId(cmd.getOrderId());
            } catch (Exception e) {
                log.error("Failed to rollback partial reservations: {}", e.getMessage());
            }
        }

        StockReservedReply reply = StockReservedReply.builder()
                .sagaId(cmd.getSagaId())
                .orderId(cmd.getOrderId())
                .success(allReserved)
                .failureReason(failureReason)
                .build();

        kafkaTemplate.send(SagaTopics.RESERVE_STOCK_REPLY, cmd.getOrderId(), reply);
        log.info("Sent StockReservedReply: sagaId={}, success={}", cmd.getSagaId(), allReserved);
    }

    @KafkaListener(topics = SagaTopics.RELEASE_STOCK_CMD, groupId = "inventory-saga")
    public void onReleaseStock(com.example.common.events.saga.ReleaseStockCommand cmd) {
        log.info("Saga {} → ReleaseStock (compensation) for order {}", cmd.getSagaId(), cmd.getOrderId());

        boolean success = true;
        try {
            releaseStockHandler.handleByOrderId(cmd.getOrderId());
        } catch (Exception e) {
            log.error("Failed to release stock for order {}: {}", cmd.getOrderId(), e.getMessage());
            success = false;
        }

        StockReleasedReply reply = StockReleasedReply.builder()
                .sagaId(cmd.getSagaId())
                .orderId(cmd.getOrderId())
                .success(success)
                .build();

        kafkaTemplate.send(SagaTopics.RELEASE_STOCK_REPLY, cmd.getOrderId(), reply);
        log.info("Sent StockReleasedReply: sagaId={}, success={}", cmd.getSagaId(), success);
    }
}
