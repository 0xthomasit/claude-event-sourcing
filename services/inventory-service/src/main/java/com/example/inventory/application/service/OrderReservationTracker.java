package com.example.inventory.application.service;

import com.example.inventory.infrastructure.persistence.entity.OrderReservationEntry;
import com.example.inventory.infrastructure.persistence.repository.JpaOrderReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tracks order → stock reservation mappings so that release/reduce
 * commands can be applied to the correct StockItem aggregates without
 * scanning the whole event store.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderReservationTracker {

    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_REDUCED  = "REDUCED";

    private final JpaOrderReservationRepository reservationRepo;

    @Transactional
    public void recordReservation(String orderId, String aggregateId, String productId, int quantity) {
        reservationRepo.save(OrderReservationEntry.builder()
                .orderId(orderId)
                .aggregateId(aggregateId)
                .productId(productId)
                .quantity(quantity)
                .status(STATUS_RESERVED)
                .build());
        log.debug("Recorded reservation: order={} product={} qty={}", orderId, productId, quantity);
    }

    @Transactional(readOnly = true)
    public List<OrderReservationEntry> findActiveReservations(String orderId) {
        return reservationRepo.findByOrderIdAndStatus(orderId, STATUS_RESERVED);
    }

    @Transactional
    public void markStatus(OrderReservationEntry entry, String newStatus) {
        entry.setStatus(newStatus);
        reservationRepo.save(entry);
    }
}
