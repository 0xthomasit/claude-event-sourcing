package com.example.shipping.application.service;

import com.example.shipping.domain.model.Shipment;
import com.example.shipping.domain.model.ShipmentStatus;
import com.example.shipping.domain.model.ShipmentTrackingEvent;
import com.example.shipping.domain.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final ShipmentRepository shipmentRepository;

    // ─── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public Shipment createShipment(String orderId, String customerId,
                                   String recipientName, String recipientPhone,
                                   String address, String district, String province) {
        if (shipmentRepository.findByOrderId(orderId).isPresent())
            throw new IllegalStateException("Shipment already exists for orderId: " + orderId);

        Shipment shipment = Shipment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId(customerId)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .address(address)
                .district(district)
                .province(province)
                .status(ShipmentStatus.PENDING)
                .build();

        shipmentRepository.save(shipment);
        log.info("Shipment created: id={}, orderId={}", shipment.getId(), orderId);
        return shipment;
    }

    @Transactional
    public Shipment updateTracking(String orderId, String trackingCode,
                                   ShipmentStatus status, String location, String description) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found for orderId: " + orderId));

        shipment.setStatus(status);
        if (trackingCode != null) shipment.setTrackingCode(trackingCode);
        if (status == ShipmentStatus.DELIVERED) shipment.setDeliveredAt(Instant.now());

        ShipmentTrackingEvent event = ShipmentTrackingEvent.builder()
                .shipment(shipment)
                .status(status.name())
                .location(location)
                .description(description)
                .occurredAt(Instant.now())
                .build();
        shipment.getTrackingEvents().add(event);

        shipmentRepository.save(shipment);
        log.info("Shipment updated: orderId={}, status={}", orderId, status);
        return shipment;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Shipment getByOrderId(String orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found for orderId: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Shipment> getByCustomerId(String customerId) {
        return shipmentRepository.findByCustomerId(customerId);
    }
}
