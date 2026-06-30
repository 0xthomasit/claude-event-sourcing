package com.example.order.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.domain.exception.AggregateNotFoundException;
import com.example.common.events.order.OrderConfirmedEvent;
import com.example.order.application.command.dto.ConfirmOrderCommand;
import com.example.order.domain.model.Order;
import com.example.order.domain.repository.OrderRepository;
import com.example.order.infrastructure.messaging.projector.OrderProjector;
import com.example.order.infrastructure.messaging.publisher.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmOrderHandler {

    private final OrderRepository orderRepository;
    private final OrderProjector  orderProjector;
    private final KafkaEventPublisher kafkaPublisher;
    private final Executor taskExecutor;

    public void handle(ConfirmOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new AggregateNotFoundException(
                        "Order not found: " + command.orderId()));

        // Fan-out: parallel validation — max(t1,t2,t3) instead of t1+t2+t3
        runParallelValidation(order);

        order.confirm();

        List<DomainEvent> events = List.copyOf(order.getUncommittedEvents());
        orderRepository.save(order);

        events.stream()
                .filter(e -> e instanceof OrderConfirmedEvent)
                .map(e -> (OrderConfirmedEvent) e)
                .forEach(orderProjector::on);

        kafkaPublisher.publishAll(order.getId().toString(), events);
        log.info("Order confirmed: {}", order.getId());
    }

    /**
     * Fan-out / Fan-in: 3 independent checks run in parallel.
     * Total wait = max(check1, check2, check3), not sum.
     * Hard timeout = 3 seconds; cancels all futures on timeout.
     */
    private void runParallelValidation(Order order) {
        CompletableFuture<Void> inventoryCheck = CompletableFuture.runAsync(() -> {
            // TODO: RestTemplate/Feign call to inventory-service
            log.debug("Inventory check passed for order {}", order.getId());
        }, taskExecutor);
        CompletableFuture<Void> paymentCheck = CompletableFuture.runAsync(() -> {
            // TODO: RestTemplate/Feign call to payment-service eligibility
            log.debug("Payment eligibility check passed for order {}", order.getId());
        }, taskExecutor);
        CompletableFuture<Void> addressCheck = CompletableFuture.runAsync(() -> {
            // TODO: RestTemplate/Feign call to user-service address validation
            log.debug("Address check passed for order {}", order.getId());
        }, taskExecutor);

        try {
            CompletableFuture.allOf(inventoryCheck, paymentCheck, addressCheck)
                    .get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            inventoryCheck.cancel(true);
            paymentCheck.cancel(true);
            addressCheck.cancel(true);
            throw new IllegalStateException("Order validation timed out or failed: " + e.getMessage(), e);
        }
    }
}
