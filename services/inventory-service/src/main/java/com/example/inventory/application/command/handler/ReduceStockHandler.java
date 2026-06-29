package com.example.inventory.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.events.inventory.StockReducedEvent;
import com.example.inventory.application.command.dto.ReduceStockCommand;
import com.example.inventory.application.service.OrderReservationTracker;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.repository.StockItemRepository;
import com.example.inventory.infrastructure.messaging.projector.StockItemProjector;
import com.example.inventory.infrastructure.messaging.publisher.KafkaEventPublisher;
import com.example.inventory.infrastructure.persistence.entity.OrderReservationEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReduceStockHandler {

    private final StockItemRepository stockItemRepository;
    private final StockItemProjector  projector;
    private final KafkaEventPublisher kafkaPublisher;
    private final OrderReservationTracker reservationTracker;

    @Transactional
    public void handle(ReduceStockCommand command) {
        StockItem stockItem = stockItemRepository.findByProductId(command.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                        "No stock item found for product: " + command.getProductId()));

        stockItem.reduce(command.getOrderId(), command.getQuantity());

        List<DomainEvent> events = List.copyOf(stockItem.getUncommittedEvents());
        stockItemRepository.save(stockItem);

        events.stream()
                .filter(e -> e instanceof StockReducedEvent)
                .map(e -> (StockReducedEvent) e)
                .forEach(projector::on);

        kafkaPublisher.publishAll(stockItem.getId().toString(), events);
        log.info("Stock reduced: product={} order={} qty={}",
                command.getProductId(), command.getOrderId(), command.getQuantity());
    }

    /**
     * Permanently reduce all reserved stock for a shipped order,
     * using the reservation tracking table to locate each product.
     */
    @Transactional
    public void handleByOrderId(String orderId) {
        List<OrderReservationEntry> reservations = reservationTracker.findActiveReservations(orderId);
        if (reservations.isEmpty()) {
            log.info("No active reservations to reduce for order {}", orderId);
            return;
        }
        log.info("Reducing {} reservation(s) for shipped order {}", reservations.size(), orderId);

        for (OrderReservationEntry r : reservations) {
            try {
                handle(ReduceStockCommand.builder()
                        .productId(r.getProductId())
                        .orderId(orderId)
                        .quantity(r.getQuantity())
                        .build());
                reservationTracker.markStatus(r, OrderReservationTracker.STATUS_REDUCED);
            } catch (Exception e) {
                log.error("Failed to reduce reservation for product {} order {}: {}",
                        r.getProductId(), orderId, e.getMessage());
            }
        }
    }
}
