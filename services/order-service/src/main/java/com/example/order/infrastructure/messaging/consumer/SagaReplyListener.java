package com.example.order.infrastructure.messaging.consumer;

import com.example.common.events.saga.*;
import com.example.order.application.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to all saga reply topics and routes them
 * to the {@link OrderSagaOrchestrator} for state transitions.
 *
 * <p>This replaces the old choreography-based {@code PaymentEventConsumer}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaReplyListener {

    private final OrderSagaOrchestrator orchestrator;

    @KafkaListener(topics = SagaTopics.RESERVE_STOCK_REPLY, groupId = "order-saga")
    public void onStockReservedReply(StockReservedReply reply) {
        log.info("Received StockReservedReply: sagaId={}, success={}", reply.getSagaId(), reply.isSuccess());
        try {
            orchestrator.onStockReservedReply(reply);
        } catch (Exception e) {
            log.error("Failed to process StockReservedReply for saga {}: {}",
                    reply.getSagaId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SagaTopics.PROCESS_PAYMENT_REPLY, groupId = "order-saga")
    public void onPaymentProcessedReply(PaymentProcessedReply reply) {
        log.info("Received PaymentProcessedReply: sagaId={}, success={}", reply.getSagaId(), reply.isSuccess());
        try {
            orchestrator.onPaymentProcessedReply(reply);
        } catch (Exception e) {
            log.error("Failed to process PaymentProcessedReply for saga {}: {}",
                    reply.getSagaId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = SagaTopics.RELEASE_STOCK_REPLY, groupId = "order-saga")
    public void onStockReleasedReply(StockReleasedReply reply) {
        log.info("Received StockReleasedReply: sagaId={}, success={}", reply.getSagaId(), reply.isSuccess());
        try {
            orchestrator.onStockReleasedReply(reply);
        } catch (Exception e) {
            log.error("Failed to process StockReleasedReply for saga {}: {}",
                    reply.getSagaId(), e.getMessage(), e);
        }
    }
}
