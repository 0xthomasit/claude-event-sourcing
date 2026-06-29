package com.example.payment.infrastructure.messaging.publisher;

import com.example.common.events.payment.*;
import com.example.common.domain.DomainEvent;
import com.example.common.events.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes payment domain events to Kafka.
 * Events are persisted to Event Store FIRST, then published here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAll(String aggregateId, List<DomainEvent> events) {
        for (DomainEvent event : events) {
            if (event instanceof PaymentCompletedEvent e) publish(e);
            else if (event instanceof PaymentFailedEvent e) publish(e);
            else if (event instanceof PaymentInitiatedEvent e) publish(e);
        }
    }

    public void publish(PaymentCompletedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, event.getAggregateId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentCompletedEvent for order={}: {}",
                                event.getOrderId(), ex.getMessage());
                    } else {
                        log.info("Published PaymentCompletedEvent: paymentId={}, orderId={}",
                                event.getAggregateId(), event.getOrderId());
                    }
                });
    }

    public void publish(PaymentFailedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED, event.getAggregateId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentFailedEvent for order={}: {}",
                                event.getOrderId(), ex.getMessage());
                    } else {
                        log.info("Published PaymentFailedEvent: paymentId={}, orderId={}, reason={}",
                                event.getAggregateId(), event.getOrderId(), event.getFailureReason());
                    }
                });
    }

    public void publish(PaymentInitiatedEvent event) {
        log.info("Payment initiated: paymentId={}, orderId={}",
                event.getAggregateId(), event.getOrderId());
    }
}
