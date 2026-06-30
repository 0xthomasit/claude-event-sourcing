package com.example.product.infrastructure.messaging;

import com.example.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Publishes product domain events to Kafka.
 * product.price.updated → consumed by cart-service and order-service for cache-busting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventPublisher {

    static final String TOPIC_PRICE_UPDATED = "product.price.updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPriceUpdated(Product product, BigDecimal oldPrice) {
        Map<String, Object> event = Map.of(
                "eventType",  "PRODUCT_PRICE_UPDATED",
                "productId",  product.getId().toString(),
                "sku",        product.getSku(),
                "oldPrice",   oldPrice,
                "newPrice",   product.getPrice(),
                "currency",   product.getCurrency(),
                "occurredOn", Instant.now().toString()
        );

        kafkaTemplate.send(TOPIC_PRICE_UPDATED, product.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish price update for product {}: {}",
                                product.getId(), ex.getMessage());
                    } else {
                        log.debug("Published price update for product {} → topic {}",
                                product.getId(), TOPIC_PRICE_UPDATED);
                    }
                });
    }
}
