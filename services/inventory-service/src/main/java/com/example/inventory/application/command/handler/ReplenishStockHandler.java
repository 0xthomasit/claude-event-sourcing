package com.example.inventory.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.events.inventory.StockReplenishedEvent;
import com.example.inventory.application.command.dto.ReplenishStockCommand;
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
public class ReplenishStockHandler {

    private final StockItemRepository stockItemRepository;
    private final StockItemProjector  projector;
    private final KafkaEventPublisher kafkaPublisher;

    @Transactional
    public void handle(ReplenishStockCommand command) {
        StockItem stockItem = stockItemRepository.findByProductId(command.getProductId())
                .orElseGet(() -> {
                    // First time — initialize a new StockItem for this product
                    log.info("Initializing new StockItem for product {}", command.getProductId());
                    return StockItem.initialize(command.getProductId(), "DEFAULT", 0);
                });

        // If brand new, save first to get an ID, then replenish
        if (stockItem.getId() == null) {
            List<DomainEvent> initEvents = List.copyOf(stockItem.getUncommittedEvents());
            stockItemRepository.save(stockItem);
            initEvents.stream()
                    .filter(e -> e instanceof StockReplenishedEvent)
                    .map(e -> (StockReplenishedEvent) e)
                    .forEach(projector::on);
            kafkaPublisher.publishAll(stockItem.getId().toString(), initEvents);
        } else {
            stockItem.replenish(command.getQuantity(), command.getReference());
            List<DomainEvent> events = List.copyOf(stockItem.getUncommittedEvents());
            stockItemRepository.save(stockItem);
            events.stream()
                    .filter(e -> e instanceof StockReplenishedEvent)
                    .map(e -> (StockReplenishedEvent) e)
                    .forEach(projector::on);
            kafkaPublisher.publishAll(stockItem.getId().toString(), events);
        }

        log.info("Stock replenished: product={} qty={} ref={}",
                command.getProductId(), command.getQuantity(), command.getReference());
    }
}