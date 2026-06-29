package com.example.payment.infrastructure.persistence.repository;

import com.example.payment.infrastructure.persistence.entity.PaymentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MongoPaymentRepository extends MongoRepository<PaymentDocument, String> {

    Optional<PaymentDocument> findByOrderId(String orderId);

    List<PaymentDocument> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
