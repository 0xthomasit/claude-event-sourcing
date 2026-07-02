package com.example.payment.infrastructure.persistence.repository;

import com.example.payment.infrastructure.persistence.entity.SagaPaymentCorrelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SagaPaymentCorrelationRepository extends JpaRepository<SagaPaymentCorrelation, String> {

    Optional<SagaPaymentCorrelation> findByOrderId(String orderId);
}
