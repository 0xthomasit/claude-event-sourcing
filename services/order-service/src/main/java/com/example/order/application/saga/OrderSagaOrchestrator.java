package com.example.order.application.saga;

import com.example.common.domain.DomainEvent;
import com.example.common.domain.exception.AggregateNotFoundException;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.common.events.order.OrderConfirmedEvent;
import com.example.common.events.saga.*;
import com.example.order.domain.model.Order;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.messaging.projector.OrderProjector;
import com.example.order.infrastructure.messaging.publisher.KafkaEventPublisher;
import com.example.order.infrastructure.metrics.OrderMetrics;
import com.example.order.infrastructure.persistence.entity.SagaInstance;
import com.example.order.infrastructure.persistence.repository.JpaSagaRepository;
import com.example.order.infrastructure.saga.SagaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Central Saga Orchestrator for the Order Placement flow.
 *
 * <p>Manages the saga state machine:
 * <pre>
 * STARTED → RESERVING_STOCK → STOCK_RESERVED → PROCESSING_PAYMENT
 *         → PAYMENT_COMPLETED → CONFIRMING_ORDER → COMPLETED
 *
 * Compensation:
 *   STOCK_RESERVATION_FAILED → REJECTED
 *   PAYMENT_FAILED → COMPENSATING_STOCK → COMPENSATED
 * </pre>
 *
 * <p>Each step transition is persisted to the saga_instances table within
 * a database transaction, ensuring the saga state survives process crashes.
 *
 * <p>Kafka sends are retried up to {@code saga.max-retries} times before
 * triggering compensation. See {@link SagaProperties}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private static final String SAGA_TYPE = "OrderPlacementSaga";

    private final JpaSagaRepository sagaRepository;
    private final OrderRepository orderRepository;
    private final OrderProjector orderProjector;
    private final KafkaEventPublisher kafkaPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderMetrics orderMetrics;
    private final ObjectMapper objectMapper;
    private final SagaProperties sagaProperties;

    // ─── Step 1: Start Saga ─────────────────────────────────────────────────

    /**
     * Starts the saga after an order is placed.
     * Persists saga state and sends ReserveStockCommand to inventory-service.
     */
    @Transactional
    public void startSaga(UUID orderId, OrderSagaData sagaData) {
        UUID sagaId = UUID.randomUUID();

        SagaInstance saga = SagaInstance.builder()
                .id(sagaId)
                .sagaType(SAGA_TYPE)
                .orderId(orderId.toString())
                .currentStep(OrderSagaStep.RESERVING_STOCK.name())
                .payload(serialize(sagaData))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        sagaRepository.save(saga);

        List<ReserveStockCommand.ItemToReserve> items = sagaData.getItems().stream()
                .map(i -> ReserveStockCommand.ItemToReserve.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        ReserveStockCommand command = ReserveStockCommand.builder()
                .sagaId(sagaId.toString())
                .orderId(orderId.toString())
                .items(items)
                .build();

        sendWithRetry(SagaTopics.RESERVE_STOCK_CMD, orderId.toString(), command,
                "ReserveStockCommand", saga, sagaData);
        log.info("Saga {} started for order {}, step=RESERVING_STOCK", sagaId, orderId);
    }

    // ─── Step 2: Handle Stock Reserved Reply ────────────────────────────────

    @Transactional
    public void onStockReservedReply(StockReservedReply reply) {
        SagaInstance saga = findSaga(reply.getSagaId());
        if (saga == null) return;

        OrderSagaData data = deserialize(saga.getPayload());

        if (reply.isSuccess()) {
            // Stock reserved → proceed to payment
            data = data.withStockReserved(true);
            updateSaga(saga, OrderSagaStep.PROCESSING_PAYMENT, data);

            ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                    .sagaId(saga.getId().toString())
                    .orderId(data.getOrderId())
                    .customerId(data.getCustomerId())
                    .amount(data.getTotalAmount())
                    .currency(data.getCurrency())
                    .build();

            sendWithRetry(SagaTopics.PROCESS_PAYMENT_CMD, data.getOrderId(), command,
                    "ProcessPaymentCommand", saga, data);
            log.info("Saga {} stock reserved, step=PROCESSING_PAYMENT", saga.getId());
        } else {
            // Stock reservation failed → reject order (no compensation needed)
            data = data.withFailureReason(reply.getFailureReason());
            updateSaga(saga, OrderSagaStep.REJECTED, data);
            completeSaga(saga, reply.getFailureReason());

            cancelOrder(data.getOrderId(), "Stock reservation failed: " + reply.getFailureReason());
            log.info("Saga {} rejected: stock reservation failed", saga.getId());
        }
    }

    // ─── Step 3: Handle Payment Processed Reply ─────────────────────────────

    @Transactional
    public void onPaymentProcessedReply(PaymentProcessedReply reply) {
        SagaInstance saga = findSaga(reply.getSagaId());
        if (saga == null) return;

        OrderSagaData data = deserialize(saga.getPayload());

        if (reply.isSuccess()) {
            // Payment completed → confirm order → saga done
            data = data.withPaymentResult(true, reply.getPaymentId());
            updateSaga(saga, OrderSagaStep.CONFIRMING_ORDER, data);

            confirmOrder(data.getOrderId());

            updateSaga(saga, OrderSagaStep.COMPLETED, data);
            completeSaga(saga, null);
            log.info("Saga {} completed successfully for order {}", saga.getId(), data.getOrderId());
        } else {
            // Payment failed → compensate stock → cancel order
            data = data.withFailureReason(reply.getFailureReason());
            updateSaga(saga, OrderSagaStep.COMPENSATING_STOCK, data);

            ReleaseStockCommand command = ReleaseStockCommand.builder()
                    .sagaId(saga.getId().toString())
                    .orderId(data.getOrderId())
                    .build();

            sendWithRetry(SagaTopics.RELEASE_STOCK_CMD, data.getOrderId(), command,
                    "ReleaseStockCommand (compensation)", saga, data);
            log.info("Saga {} payment failed, step=COMPENSATING_STOCK", saga.getId());
        }
    }

    // ─── Step 4: Handle Stock Released Reply (Compensation) ─────────────────

    @Transactional
    public void onStockReleasedReply(StockReleasedReply reply) {
        SagaInstance saga = findSaga(reply.getSagaId());
        if (saga == null) return;

        OrderSagaData data = deserialize(saga.getPayload());

        updateSaga(saga, OrderSagaStep.COMPENSATED, data);
        completeSaga(saga, data.getFailureReason());

        cancelOrder(data.getOrderId(), "Payment failed: " + data.getFailureReason());
        log.info("Saga {} compensated, order {} cancelled", saga.getId(), data.getOrderId());
    }

    // ─── Timeout Compensation ───────────────────────────────────────────────

    /**
     * Called by SagaTimeoutDetector for sagas stuck at non-terminal steps.
     * Initiates compensation based on what has already been done.
     */
    @Transactional
    public void compensateTimedOutSaga(SagaInstance saga) {
        OrderSagaData data = deserialize(saga.getPayload());

        if (data.isStockReserved()) {
            // Need to release stock
            updateSaga(saga, OrderSagaStep.COMPENSATING_STOCK, data.withFailureReason("Saga timeout"));

            ReleaseStockCommand command = ReleaseStockCommand.builder()
                    .sagaId(saga.getId().toString())
                    .orderId(data.getOrderId())
                    .build();
            sendWithRetry(SagaTopics.RELEASE_STOCK_CMD, data.getOrderId(), command,
                    "ReleaseStockCommand (timeout)", saga, data);
        } else {
            // Nothing to compensate — just reject
            updateSaga(saga, OrderSagaStep.REJECTED, data.withFailureReason("Saga timeout"));
            completeSaga(saga, "Saga timeout");
            cancelOrder(data.getOrderId(), "Order processing timed out");
        }

        log.warn("Saga {} timed out at step {}, compensating...",
                saga.getId(), saga.getCurrentStep());
    }

    // ─── Kafka Send with Retry ──────────────────────────────────────────────

    /**
     * Sends a message to Kafka with configurable retry.
     * After {@code saga.max-retries} failures, triggers compensation.
     *
     * @param topic       Kafka topic
     * @param key         partition key
     * @param message     message payload
     * @param commandName human-readable name for logging
     * @param saga        current saga instance (for compensation on exhaustion)
     * @param data        current saga data (for compensation on exhaustion)
     */
    private void sendWithRetry(String topic, String key, Object message,
                               String commandName, SagaInstance saga, OrderSagaData data) {
        int maxRetries = sagaProperties.getMaxRetries();
        long retryDelayMs = sagaProperties.getRetryDelay().toMillis();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                kafkaTemplate.send(topic, key, message).get();   // blocking send
                return; // success
            } catch (Exception e) {
                log.warn("Saga {} — {} send attempt {}/{} failed: {}",
                        saga.getId(), commandName, attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All retries exhausted → compensate
        log.error("Saga {} — {} send failed after {} retries, compensating...",
                saga.getId(), commandName, maxRetries);

        data = data.withFailureReason("Kafka send failed after " + maxRetries + " retries: " + commandName);

        if (data.isStockReserved() && !topic.equals(SagaTopics.RELEASE_STOCK_CMD)) {
            // Need to release stock — but we can't send to Kafka either!
            // Mark as stuck and let timeout detector handle it later
            updateSaga(saga, OrderSagaStep.COMPENSATING_STOCK, data);
            log.error("Saga {} stuck — cannot reach Kafka for compensation. Timeout detector will retry.",
                    saga.getId());
        } else {
            // No compensation needed or this IS the compensation → just reject
            updateSaga(saga, OrderSagaStep.REJECTED, data);
            completeSaga(saga, data.getFailureReason());
            cancelOrder(data.getOrderId(), "Saga command delivery failed: " + commandName);
        }
    }

    // ─── Internal Helpers ───────────────────────────────────────────────────

    private void confirmOrder(String orderId) {
        Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new AggregateNotFoundException("Order not found: " + orderId));

        order.confirm();

        List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());
        orderRepository.save(order);

        events.stream()
                .filter(e -> e instanceof OrderConfirmedEvent)
                .map(e -> (OrderConfirmedEvent) e)
                .forEach(orderProjector::on);

        kafkaPublisher.publishAll(orderId, events);
        orderMetrics.orderConfirmed();
    }

    private void cancelOrder(String orderId, String reason) {
        Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new AggregateNotFoundException("Order not found: " + orderId));

        try {
            order.cancel(reason);

            List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());
            orderRepository.save(order);

            events.stream()
                    .filter(e -> e instanceof OrderCancelledEvent)
                    .map(e -> (OrderCancelledEvent) e)
                    .forEach(orderProjector::on);

            kafkaPublisher.publishAll(orderId, events);
            orderMetrics.orderCancelled();
        } catch (IllegalStateException e) {
            // Order may already be cancelled or in a non-cancellable state
            log.warn("Could not cancel order {}: {}", orderId, e.getMessage());
        }
    }

    private SagaInstance findSaga(String sagaId) {
        return sagaRepository.findById(UUID.fromString(sagaId)).orElseGet(() -> {
            log.warn("Saga not found: {}", sagaId);
            return null;
        });
    }

    private void updateSaga(SagaInstance saga, OrderSagaStep step, OrderSagaData data) {
        saga.setCurrentStep(step.name());
        saga.setPayload(serialize(data));
        saga.setUpdatedAt(Instant.now());
        sagaRepository.save(saga);
    }

    private void completeSaga(SagaInstance saga, String failureReason) {
        saga.setCompletedAt(Instant.now());
        saga.setFailureReason(failureReason);
        sagaRepository.save(saga);
    }

    private String serialize(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException("Saga serialize failed", e); }
    }

    private OrderSagaData deserialize(String json) {
        try { return objectMapper.readValue(json, OrderSagaData.class); }
        catch (Exception e) { throw new RuntimeException("Saga deserialize failed", e); }
    }
}
