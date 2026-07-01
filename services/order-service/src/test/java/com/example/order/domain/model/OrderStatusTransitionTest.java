package com.example.order.domain.model;

import com.example.common.domain.model.Money;
import com.example.common.events.order.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order Status Transitions")
class OrderStatusTransitionTest {

    private Order createPendingOrder() {
        return Order.place("customer-001", List.of(
                OrderItem.of("prod-1", "Test Product", 1,
                        Money.of(new BigDecimal("100000"), "VND"))));
    }

    // ── Valid transitions ──────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING → CONFIRMED via confirm()")
    void pending_to_confirmed() {
        Order order = createPendingOrder();
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("PENDING → CANCELLED via cancel()")
    void pending_to_cancelled() {
        Order order = createPendingOrder();
        order.cancel("Changed my mind");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("CONFIRMED → SHIPPED via markShipped()")
    void confirmed_to_shipped() {
        Order order = createPendingOrder();
        order.confirm();
        order.markShipped("TRACK-123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("CONFIRMED → CANCELLED via cancel()")
    void confirmed_to_cancelled() {
        Order order = createPendingOrder();
        order.confirm();
        order.cancel("Out of stock");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("SHIPPED → DELIVERED via markDelivered()")
    void shipped_to_delivered() {
        Order order = createPendingOrder();
        order.confirm();
        order.markShipped("TRACK-123");
        order.markDelivered();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    // ── Invalid transitions ───────────────────────────────────────────────────

    @Test
    @DisplayName("CONFIRMED → CONFIRMED throws (double confirm)")
    void confirmed_to_confirmed_throws() {
        Order order = createPendingOrder();
        order.confirm();
        assertThatIllegalStateException().isThrownBy(order::confirm);
    }

    @Test
    @DisplayName("SHIPPED → CANCELLED throws (too late)")
    void shipped_to_cancelled_throws() {
        Order order = createPendingOrder();
        order.confirm();
        order.markShipped("TRACK-123");
        assertThatIllegalStateException().isThrownBy(() -> order.cancel("Too late"));
    }

    @Test
    @DisplayName("DELIVERED → CANCELLED throws (already delivered)")
    void delivered_to_cancelled_throws() {
        Order order = createPendingOrder();
        order.confirm();
        order.markShipped("TRACK-123");
        order.markDelivered();
        assertThatIllegalStateException().isThrownBy(() -> order.cancel("Nope"));
    }

    @Test
    @DisplayName("PENDING → SHIPPED throws (must confirm first)")
    void pending_to_shipped_throws() {
        Order order = createPendingOrder();
        assertThatIllegalStateException().isThrownBy(() -> order.markShipped("TRACK-123"));
    }

    @Test
    @DisplayName("PENDING → DELIVERED throws (must ship first)")
    void pending_to_delivered_throws() {
        Order order = createPendingOrder();
        assertThatIllegalStateException().isThrownBy(order::markDelivered);
    }

    @Test
    @DisplayName("CONFIRMED → DELIVERED throws (must ship first)")
    void confirmed_to_delivered_throws() {
        Order order = createPendingOrder();
        order.confirm();
        assertThatIllegalStateException().isThrownBy(order::markDelivered);
    }

    @Test
    @DisplayName("CANCELLED → CONFIRMED throws (terminal state)")
    void cancelled_to_confirmed_throws() {
        Order order = createPendingOrder();
        order.cancel("Cancelled");
        assertThatIllegalStateException().isThrownBy(order::confirm);
    }

    // ── Event emission ────────────────────────────────────────────────────────

    @Test
    @DisplayName("markShipped() raises OrderShippedEvent with trackingNumber")
    void markShipped_raisesEvent() {
        Order order = createPendingOrder();
        order.confirm();
        order.clearUncommittedEvents();

        order.markShipped("TRACK-999");

        assertThat(order.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(OrderShippedEvent.class);
        OrderShippedEvent event = (OrderShippedEvent) order.getUncommittedEvents().getFirst();
        assertThat(event.getTrackingNumber()).isEqualTo("TRACK-999");
    }

    @Test
    @DisplayName("markDelivered() raises OrderDeliveredEvent")
    void markDelivered_raisesEvent() {
        Order order = createPendingOrder();
        order.confirm();
        order.markShipped("TRACK-123");
        order.clearUncommittedEvents();

        order.markDelivered();

        assertThat(order.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(OrderDeliveredEvent.class);
    }

    // ── Full lifecycle ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: PENDING → CONFIRMED → SHIPPED → DELIVERED")
    void fullLifecycle() {
        Order order = createPendingOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getVersion()).isEqualTo(1);

        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getVersion()).isEqualTo(2);

        order.markShipped("GHN-ABC123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getVersion()).isEqualTo(3);

        order.markDelivered();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getVersion()).isEqualTo(4);

        // Should have 4 events total
        assertThat(order.getUncommittedEvents()).hasSize(4);
    }
}
