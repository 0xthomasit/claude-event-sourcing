package com.example.inventory.domain.repository;

import com.example.inventory.domain.model.StockItem;

import java.util.Optional;
import java.util.UUID;

/** Domain port — infrastructure implements this. */
public interface StockItemRepository {
    void save(StockItem stockItem);
    Optional<StockItem> findById(UUID id);
    Optional<StockItem> findByProductId(String productId);
}
