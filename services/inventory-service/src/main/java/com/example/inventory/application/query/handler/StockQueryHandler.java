package com.example.inventory.application.query.handler;

import com.example.inventory.application.query.dto.StockLevelResponse;
import com.example.inventory.infrastructure.persistence.entity.StockItemDocument;
import com.example.inventory.infrastructure.persistence.repository.MongoStockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockQueryHandler {

    private final MongoStockItemRepository mongoRepo;

    public Optional<StockLevelResponse> findByProductId(String productId) {
        return mongoRepo.findByProductId(productId).map(this::toResponse);
    }

    public boolean isAvailable(String productId, int requiredQty) {
        return mongoRepo.findByProductId(productId)
                .map(doc -> doc.getAvailableQuantity() >= requiredQty)
                .orElse(false);
    }

    private StockLevelResponse toResponse(StockItemDocument doc) {
        return StockLevelResponse.builder()
                .id(doc.getId())
                .productId(doc.getProductId())
                .warehouseId(doc.getWarehouseId())
                .availableQuantity(doc.getAvailableQuantity())
                .reservedQuantity(doc.getReservedQuantity())
                .totalQuantity(doc.getTotalQuantity())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}