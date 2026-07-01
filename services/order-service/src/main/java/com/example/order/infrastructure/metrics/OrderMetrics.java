package com.example.order.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer metrics for order business KPIs.
 * <p>
 * Counters:
 * <ul>
 *   <li>{@code orders.placed.total}     — incremented on every OrderPlacedEvent</li>
 *   <li>{@code orders.confirmed.total}  — incremented on every OrderConfirmedEvent</li>
 *   <li>{@code orders.cancelled.total}  — incremented on every OrderCancelledEvent</li>
 *   <li>{@code orders.shipped.total}    — incremented on every OrderShippedEvent</li>
 *   <li>{@code orders.delivered.total}  — incremented on every OrderDeliveredEvent</li>
 * </ul>
 * Exposed at {@code /actuator/prometheus} for Grafana dashboards.
 */
@Component
public class OrderMetrics {

    private final Counter placedCounter;
    private final Counter confirmedCounter;
    private final Counter cancelledCounter;
    private final Counter shippedCounter;
    private final Counter deliveredCounter;

    public OrderMetrics(MeterRegistry registry) {
        this.placedCounter    = Counter.builder("orders.placed.total")
                .description("Total number of orders placed")
                .register(registry);
        this.confirmedCounter = Counter.builder("orders.confirmed.total")
                .description("Total number of orders confirmed")
                .register(registry);
        this.cancelledCounter = Counter.builder("orders.cancelled.total")
                .description("Total number of orders cancelled")
                .register(registry);
        this.shippedCounter   = Counter.builder("orders.shipped.total")
                .description("Total number of orders shipped")
                .register(registry);
        this.deliveredCounter = Counter.builder("orders.delivered.total")
                .description("Total number of orders delivered")
                .register(registry);
    }

    public void orderPlaced()    { placedCounter.increment();    }
    public void orderConfirmed() { confirmedCounter.increment(); }
    public void orderCancelled() { cancelledCounter.increment(); }
    public void orderShipped()   { shippedCounter.increment();   }
    public void orderDelivered() { deliveredCounter.increment(); }
}
