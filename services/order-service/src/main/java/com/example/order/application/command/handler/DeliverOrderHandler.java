package com.example.order.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.domain.exception.AggregateNotFoundException;
import com.example.common.events.order.OrderDeliveredEvent;
import com.example.order.application.command.dto.DeliverOrderCommand;
import com.example.order.domain.model.Order;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.messaging.projector.OrderProjector;
import com.example.order.infrastructure.messaging.publisher.KafkaEventPublisher;
import com.example.order.infrastructure.metrics.OrderMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliverOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderProjector  orderProjector;
    private final KafkaEventPublisher kafkaPublisher;
    private final OrderMetrics orderMetrics;

    @Transactional
    public void handle(DeliverOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new AggregateNotFoundException(
                        "Order not found: " + command.orderId()));

        order.markDelivered();

        List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());
        orderRepository.save(order);

        events.stream()
                .filter(e -> e instanceof OrderDeliveredEvent)
                .map(e -> (OrderDeliveredEvent) e)
                .forEach(orderProjector::on);

        kafkaPublisher.publishAll(order.getId().toString(), events);

        orderMetrics.orderDelivered();
        log.info("Order delivered: {}", order.getId());
    }
}
