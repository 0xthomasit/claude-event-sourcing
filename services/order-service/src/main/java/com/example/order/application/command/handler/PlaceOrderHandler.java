package com.example.order.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.domain.model.Money;
import com.example.common.events.order.OrderPlacedEvent;
import com.example.order.application.command.dto.PlaceOrderCommand;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.messaging.projector.OrderProjector;
import com.example.order.infrastructure.messaging.publisher.KafkaEventPublisher;
import com.example.order.infrastructure.metrics.OrderMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderProjector  orderProjector;
    private final KafkaEventPublisher kafkaPublisher;
    private final OrderMetrics orderMetrics;

    @Transactional
    public UUID handle(PlaceOrderCommand command) {
        List<OrderItem> items = command.getItems().stream()
                .map(i -> OrderItem.of(i.getProductId(), i.getProductName(),
                        i.getQuantity(), Money.of(i.getUnitPrice(), i.getCurrency())))
                .toList();

        Order order = Order.place(command.getCustomerId(), items);

        // Capture events BEFORE save clears them
        List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());

        // 1. Persist to Event Store (source of truth)
        orderRepository.save(order);

        // 2. Update Read Model (MongoDB)
        events.stream()
                .filter(e -> e instanceof OrderPlacedEvent)
                .map(e -> (OrderPlacedEvent) e)
                .forEach(orderProjector::on);

        // 3. Publish to Kafka (after persist — never before)
        kafkaPublisher.publishAll(order.getId().toString(), events);

        orderMetrics.orderPlaced();
        log.info("Order placed: {}", order.getId());
        return order.getId();
    }
}
