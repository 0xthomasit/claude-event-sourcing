package com.example.inventory.infrastructure.persistence.repository;

import com.example.inventory.infrastructure.persistence.entity.StockItemDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MongoStockItemRepository extends MongoRepository<StockItemDocument, String> {
    Optional<StockItemDocument> findByProductId(String productId);
}