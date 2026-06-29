package com.example.inventory.infrastructure.messaging.publisher;

import com.example.common.domain.DomainEvent;
import com.example.common.events.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

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
            log.warn("No topic mapped for event type: {}", event.getEventType());
            return;
        }
        kafkaTemplate.send(topic, aggregateId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null)
                        log.error("Failed to publish {} to {}: {}", event.getEventType(), topic, ex.getMessage());
                    else
                        log.debug("Published {} → {} partition {}",
                                event.getEventType(), topic,
                                result.getRecordMetadata().partition());
                });
    }

    private String resolveTopic(DomainEvent event) {
        return switch (event.getEventType()) {
            case "STOCK_RESERVED"    -> KafkaTopics.STOCK_RESERVED;
            case "STOCK_RELEASED"    -> KafkaTopics.STOCK_RELEASED;
            case "STOCK_REDUCED"     -> KafkaTopics.STOCK_REDUCED;
            case "STOCK_REPLENISHED" -> KafkaTopics.STOCK_REPLENISHED;
            case "STOCK_ADJUSTED"    -> KafkaTopics.STOCK_ADJUSTED;
            default -> null;
        };
    }
}