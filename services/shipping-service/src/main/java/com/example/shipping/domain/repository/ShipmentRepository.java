
package com.example.shipping.domain.repository;

import com.example.shipping.domain.model.Shipment;
import com.example.shipping.domain.model.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {

    Optional<Shipment> findByOrderId(String orderId);

    List<Shipment> findByCustomerId(String customerId);

    List<Shipment> findByStatus(ShipmentStatus status);
}
