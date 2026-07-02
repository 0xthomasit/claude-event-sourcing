package com.example.order.infrastructure.saga;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configurable saga properties.
 *
 * <pre>
 * saga:
 *   timeout: 5m          # how long before a saga is considered stalled
 *   poll-interval: 30s   # how often to check for stalled sagas
 *   max-retries: 3       # Kafka send retries before compensating
 *   retry-delay: 1s      # delay between retries
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "saga")
@Getter
@Setter
public class SagaProperties {

    /** How long a saga can be in a non-terminal step before it's considered stalled. */
    private Duration timeout = Duration.ofMinutes(5);

    /** How often the timeout detector runs. */
    private Duration pollInterval = Duration.ofSeconds(30);

    /** Max Kafka send retries before the orchestrator triggers compensation. */
    private int maxRetries = 3;

    /** Delay between retry attempts. */
    private Duration retryDelay = Duration.ofSeconds(1);
}
