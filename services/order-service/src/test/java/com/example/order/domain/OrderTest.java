package com.example.order.domain;

import com.example.common.domain.model.Money;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.common.events.order.OrderConfirmedEvent;
import com.example.common.events.order.OrderPlacedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order Aggregate")
class OrderTest {

    private List<OrderItem> validItems() {
        return List.of(
                OrderItem.of("prod-1", "iPhone 15", 2,
                        Money.of(new BigDecimal("25000000"), "VND")),
                OrderItem.of("prod-2", "Case", 1,
                        Money.of(new BigDecimal("500000"), "VND"))
        );
    }

    @Test
    @DisplayName("place() — creates PENDING order and raises OrderPlacedEvent")
    void place_success_raisesOrderPlacedEvent() {
        Order order = Order.place("customer-001", validItems());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCustomerId()).isEqualTo("customer-001");
        assertThat(order.getTotalAmount().getAmount())
                .isEqualByComparingTo(new BigDecimal("50500000"));
        assertThat(order.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(OrderPlacedEvent.class);
    }

    @Test
    @DisplayName("place() — blank customerId throws IllegalArgumentException")
    void place_blankCustomerId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Order.place("", validItems()));
    }

    @Test
    @DisplayName("place() — empty items throws IllegalArgumentException")
    void place_emptyItems_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Order.place("customer-001", List.of()));
    }

    @Test
    @DisplayName("confirm() — PENDING → CONFIRMED raises OrderConfirmedEvent")
    void confirm_whenPending_raisesOrderConfirmedEvent() {
        Order order = Order.place("customer-001", validItems());
        order.clearUncommittedEvents();

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(OrderConfirmedEvent.class);
    }

    @Test
    @DisplayName("confirm() — already CONFIRMED throws IllegalStateException")
    void confirm_whenAlreadyConfirmed_throws() {
        Order order = Order.place("customer-001", validItems());
        order.confirm();

        assertThatIllegalStateException().isThrownBy(order::confirm);
    }

    @Test
    @DisplayName("cancel() — PENDING → CANCELLED raises OrderCancelledEvent")
    void cancel_whenPending_raisesOrderCancelledEvent() {
        Order order = Order.place("customer-001", validItems());
        order.clearUncommittedEvents();

        order.cancel("Customer requested");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo("Customer requested");
        assertThat(order.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(OrderCancelledEvent.class);
    }

    @Test
    @DisplayName("cancel() — SHIPPED order throws IllegalStateException")
    void cancel_whenShipped_throws() {
        Order order = Order.place("customer-001", validItems());
        order.confirm();
        order.markShipped("TRACK-001");

        assertThatIllegalStateException()
                .isThrownBy(() -> order.cancel("Too late"));
    }

    @Test
    @DisplayName("version increments with each event applied")
    void version_incrementsWithEachEvent() {
        Order order = Order.place("customer-001", validItems());
        assertThat(order.getVersion()).isEqualTo(1);

        order.confirm();
        assertThat(order.getVersion()).isEqualTo(2);

        order.markShipped("TRACK-001");
        assertThat(order.getVersion()).isEqualTo(3);
    }
}