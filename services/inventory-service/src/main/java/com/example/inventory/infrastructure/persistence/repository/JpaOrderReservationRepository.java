package com.example.inventory.infrastructure.persistence.repository;

import com.example.inventory.infrastructure.persistence.entity.OrderReservationEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaOrderReservationRepository extends JpaRepository<OrderReservationEntry, Long> {

    List<OrderReservationEntry> findByOrderIdAndStatus(String orderId, String status);

    List<OrderReservationEntry> findByOrderId(String orderId);
}
