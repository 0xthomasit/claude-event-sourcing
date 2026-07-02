package com.example.review.infrastructure.persistence.repository;

import com.example.review.domain.model.OrderDeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaOrderDeliveryRepository extends JpaRepository<OrderDeliveryRecord, UUID> {
    Optional<OrderDeliveryRecord> findByOrderId(String orderId);
}
