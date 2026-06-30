package com.example.order.infrastructure.persistence.repository;

import com.example.order.infrastructure.persistence.entity.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoOrderRepository extends MongoRepository<OrderDocument, String> {
    List<OrderDocument> findByCustomerId(String customerId);
}
