package com.example.order.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.domain.exception.AggregateNotFoundException;
import com.example.common.events.order.OrderConfirmedEvent;
import com.example.order.application.command.dto.ConfirmOrderCommand;
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

/**
 * Confirms an order after all saga validations have passed.
 *
 * <p>Previously contained fan-out parallel validation (inventory, payment, address).
 * This validation is now handled by the Saga Orchestrator which coordinates
 * stock reservation and payment processing before calling confirm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderProjector  orderProjector;
    private final KafkaEventPublisher kafkaPublisher;
    private final OrderMetrics orderMetrics;

    @Transactional
    public void handle(ConfirmOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new AggregateNotFoundException(
                        "Order not found: " + command.orderId()));

        // Pre-validation is now done by the saga:
        //   Step 1: Stock reserved (by inventory-service via saga command)
        //   Step 2: Payment processed (by payment-service via saga command)
        // If we reach here, all validations have passed.

        order.confirm();

        List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());
        orderRepository.save(order);

        events.stream()
                .filter(e -> e instanceof OrderConfirmedEvent)
                .map(e -> (OrderConfirmedEvent) e)
                .forEach(orderProjector::on);

        kafkaPublisher.publishAll(order.getId().toString(), events);

        orderMetrics.orderConfirmed();
        log.info("Order confirmed: {}", order.getId());
    }
}
