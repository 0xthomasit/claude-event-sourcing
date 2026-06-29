package com.example.inventory.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.events.inventory.StockReleasedEvent;
import com.example.inventory.application.command.dto.ReleaseStockCommand;
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
public class ReleaseStockHandler {

    private final StockItemRepository stockItemRepository;
    private final StockItemProjector  projector;
    private final KafkaEventPublisher kafkaPublisher;
    private final OrderReservationTracker reservationTracker;

    @Transactional
    public void handle(ReleaseStockCommand command) {
        StockItem stockItem = stockItemRepository.findByProductId(command.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                        "No stock item found for product: " + command.getProductId()));

        stockItem.release(command.getOrderId(), command.getQuantity());

        List<DomainEvent> events = List.copyOf(stockItem.getUncommittedEvents());
        stockItemRepository.save(stockItem);

        events.stream()
                .filter(e -> e instanceof StockReleasedEvent)
                .map(e -> (StockReleasedEvent) e)
                .forEach(projector::on);

        kafkaPublisher.publishAll(stockItem.getId().toString(), events);
        log.info("Stock released: product={} order={} qty={}",
                command.getProductId(), command.getOrderId(), command.getQuantity());
    }

    /**
     * Release all reserved stock for a given orderId.
     * Called when order is cancelled — uses the reservation tracking table
     * to find exactly which products were reserved for this order.
     */
    @Transactional
    public void handleByOrderId(String orderId) {
        List<OrderReservationEntry> reservations = reservationTracker.findActiveReservations(orderId);
        if (reservations.isEmpty()) {
            log.info("No active reservations to release for order {}", orderId);
            return;
        }
        log.info("Releasing {} reservation(s) for cancelled order {}", reservations.size(), orderId);

        for (OrderReservationEntry r : reservations) {
            try {
                handle(ReleaseStockCommand.builder()
                        .productId(r.getProductId())
                        .orderId(orderId)
                        .quantity(r.getQuantity())
                        .build());
                reservationTracker.markStatus(r, OrderReservationTracker.STATUS_RELEASED);
            } catch (Exception e) {
                log.error("Failed to release reservation for product {} order {}: {}",
                        r.getProductId(), orderId, e.getMessage());
            }
        }
    }
}
