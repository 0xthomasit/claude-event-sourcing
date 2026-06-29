package com.example.order.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.domain.exception.AggregateNotFoundException;
import com.example.common.events.order.OrderCancelledEvent;
import com.example.order.application.command.dto.CancelOrderCommand;
import com.example.order.domain.model.Order;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.messaging.projector.OrderProjector;
import com.example.order.infrastructure.messaging.publisher.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderProjector  orderProjector;
    private final KafkaEventPublisher kafkaPublisher;

    @Transactional
    public void handle(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new AggregateNotFoundException(
                        "Order not found: " + command.orderId()));

        order.cancel(command.reason());

        List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());
        orderRepository.save(order);

        events.stream()
                .filter(e -> e instanceof OrderCancelledEvent)
                .map(e -> (OrderCancelledEvent) e)
                .forEach(orderProjector::on);

        kafkaPublisher.publishAll(order.getId().toString(), events);
        log.info("Order cancelled: {} reason: {}", order.getId(), command.reason());
    }
}