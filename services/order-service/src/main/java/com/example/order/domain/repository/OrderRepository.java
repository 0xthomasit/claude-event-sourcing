package com.example.order.domain.repository;

import com.example.order.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

/** Domain port — infrastructure implements this. */
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(UUID id);
}
