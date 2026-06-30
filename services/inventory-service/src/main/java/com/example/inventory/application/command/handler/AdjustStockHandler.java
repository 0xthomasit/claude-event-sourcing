package com.example.inventory.application.command.handler;

import com.example.common.domain.DomainEvent;
import com.example.common.events.inventory.StockAdjustedEvent;
import com.example.inventory.application.command.dto.AdjustStockCommand;
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
public class AdjustStockHandler {

    private final StockItemRepository stockItemRepository;
    private final StockItemProjector  projector;
    private final KafkaEventPublisher kafkaPublisher;

    @Transactional
    public void handle(AdjustStockCommand command) {
        StockItem stockItem = stockItemRepository.findByProductId(command.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                        "No stock item found for product: " + command.getProductId()));

        stockItem.adjust(command.getDelta(), command.getReason());

        List<DomainEvent> events = List.copyOf(stockItem.getUncommittedEvents());
        stockItemRepository.save(stockItem);

        events.stream()
                .filter(e -> e instanceof StockAdjustedEvent)
                .map(e -> (StockAdjustedEvent) e)
                .forEach(projector::on);

        kafkaPublisher.publishAll(stockItem.getId().toString(), events);
        log.info("Stock adjusted: product={} delta={} reason={}",
                command.getProductId(), command.getDelta(), command.getReason());
    }
}
