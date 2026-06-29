package com.example.order.domain.model;

import com.example.common.domain.AggregateRoot;
import com.example.common.domain.model.Money;
import com.example.common.events.order.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class Order extends AggregateRoot {

    private UUID id;
    private String customerId;
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status;
    private Money totalAmount;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;

    // Private no-arg constructor for reconstitution from events
    public Order() {}

    // ─── Factory ─────────────────────────────────────────────────────────────

    public static Order place(String customerId, List<OrderItem> items) {
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("customerId must not be blank");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Order must have at least one item");

        Order order = new Order();
        order.id = UUID.randomUUID();

        Money total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.of(BigDecimal.ZERO, "VND"), Money::add);

        List<OrderItemDto> itemDtos = items.stream()
                .map(i -> OrderItemDto.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice().getAmount())
                        .currency(i.getUnitPrice().getCurrency())
                        .build())
                .toList();

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .aggregateId(order.id.toString())
                .customerId(customerId)
                .items(itemDtos)
                .totalAmount(total.getAmount())
                .occurredOn(Instant.now())
                .build();

        order.raiseEvent(event);
        order.apply(event);
        return order;
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    public void confirm() {
        if (status != OrderStatus.PENDING)
            throw new IllegalStateException("Only PENDING orders can be confirmed, current: " + status);

        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .aggregateId(id.toString())
                .customerId(customerId)
                .totalAmount(totalAmount.getAmount())
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED)
            throw new IllegalStateException("Cannot cancel order in status: " + status);

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .aggregateId(id.toString())
                .customerId(customerId)
                .reason(reason)
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    public void markShipped(String trackingNumber) {
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.PROCESSING)
            throw new IllegalStateException("Cannot ship order in status: " + status);

        OrderShippedEvent event = OrderShippedEvent.builder()
                .aggregateId(id.toString())
                .trackingNumber(trackingNumber)
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    public void markDelivered() {
        if (status != OrderStatus.SHIPPED)
            throw new IllegalStateException("Cannot deliver order in status: " + status);

        OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                .aggregateId(id.toString())
                .occurredOn(Instant.now())
                .build();
        raiseEvent(event);
        apply(event);
    }

    // ─── Event Apply (reconstitute state from events) ─────────────────────────

    public void apply(OrderPlacedEvent e) {
        this.id          = UUID.fromString(e.getAggregateId());
        this.customerId  = e.getCustomerId();
        this.status      = OrderStatus.PENDING;
        this.totalAmount = Money.of(e.getTotalAmount(), "VND");
        this.items       = e.getItems().stream()
                .map(dto -> OrderItem.of(
                        dto.getProductId(),
                        dto.getProductName(),
                        dto.getQuantity(),
                        Money.of(dto.getUnitPrice(), dto.getCurrency())))
                .collect(Collectors.toCollection(ArrayList::new));
        this.createdAt   = e.getOccurredOn();
        this.updatedAt   = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(OrderConfirmedEvent e) {
        this.status    = OrderStatus.CONFIRMED;
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(OrderCancelledEvent e) {
        this.status             = OrderStatus.CANCELLED;
        this.cancellationReason = e.getReason();
        this.updatedAt          = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(OrderShippedEvent e) {
        this.status    = OrderStatus.SHIPPED;
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(OrderDeliveredEvent e) {
        this.status    = OrderStatus.DELIVERED;
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}