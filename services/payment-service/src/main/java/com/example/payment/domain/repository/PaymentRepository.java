package com.example.payment.domain.repository;

import com.example.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) — domain layer does NOT depend on Spring Data or JPA.
 * Implemented by infrastructure/persistence/adapter/PaymentRepositoryAdapter.
 */
public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByOrderId(String orderId);
}
