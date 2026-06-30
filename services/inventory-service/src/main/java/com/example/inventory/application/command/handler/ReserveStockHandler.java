package com.example.inventory.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.events.inventory.StockReservedEvent;
import com.example.inventory.application.command.dto.ReserveStockCommand;
import com.example.inventory.application.service.OrderReservationTracker;
import com.example.inventory.domain.model.StockItem;
import com.example.inventory.domain.repository.StockItemRepository;
import com.example.inventory.infrastructure.messaging.projector.StockItemProjector;
import com.example.inventory.infrastructure.messaging.publisher.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReserveStockHandler {

    private final StockItemRepository stockItemRepository;
    private final StockItemProjector  projector;
    private final KafkaEventPublisher kafkaPublisher;
    private final OrderReservationTracker reservationTracker;

    @Transactional
    public void handle(ReserveStockCommand command) {
        StockItem stockItem = stockItemRepository.findByProductId(command.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                        "No stock item found for product: " + command.getProductId()));

        stockItem.reserve(command.getOrderId(), command.getQuantity());

        List<DomainEvent> events = List.copyOf(stockItem.getUncommittedEvents());
        stockItemRepository.save(stockItem);

        // Track reservation for later release/reduce by orderId
        reservationTracker.recordReservation(
                command.getOrderId(),
                stockItem.getId().toString(),
                command.getProductId(),
                command.getQuantity());

        events.stream()
                .filter(e -> e instanceof StockReservedEvent)
                .map(e -> (StockReservedEvent) e)
                .forEach(projector::on);

        kafkaPublisher.publishAll(stockItem.getId().toString(), events);
        log.info("Stock reserved: product={} order={} qty={}",
                command.getProductId(), command.getOrderId(), command.getQuantity());
    }
}
