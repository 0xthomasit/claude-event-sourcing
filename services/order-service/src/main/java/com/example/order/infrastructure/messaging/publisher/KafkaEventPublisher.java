package com.example.order.infrastructure.messaging.publisher;

import com.example.common.domain.DomainEvent;
import com.example.common.events.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes domain events to Kafka AFTER they are persisted to the Event Store.
 * Rule: persist first → publish second. Never the other way around.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAll(String aggregateId, List<DomainEvent> events) {
        events.forEach(event -> publish(aggregateId, event));
    }
    private void publish(String aggregateId, DomainEvent event) {
        String topic = resolveTopic(event);
        if (topic == null) {
            log.warn("No Kafka topic mapped for event type: {}", event.getEventType());
            return;
        }
        kafkaTemplate.send(topic, aggregateId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to {}: {}", event.getEventType(), topic, ex.getMessage());
                    } else {
                        log.debug("Published {} → {} partition {}",
                                event.getEventType(), topic,
                                result.getRecordMetadata().partition());
                    }
                });
    }

    private String resolveTopic(DomainEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED"    -> KafkaTopics.ORDER_PLACED;
            case "ORDER_CONFIRMED" -> KafkaTopics.ORDER_CONFIRMED;
            case "ORDER_CANCELLED" -> KafkaTopics.ORDER_CANCELLED;
            case "ORDER_SHIPPED"   -> KafkaTopics.ORDER_SHIPPED;
            case "ORDER_DELIVERED" -> KafkaTopics.ORDER_DELIVERED;
            default -> null;
        };
    }
}
