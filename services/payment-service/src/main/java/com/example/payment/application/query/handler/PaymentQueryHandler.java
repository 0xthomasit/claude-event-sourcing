package com.example.payment.application.query.handler;

import com.example.payment.application.query.dto.PaymentResponse;
import com.example.payment.infrastructure.persistence.entity.PaymentDocument;
import com.example.payment.infrastructure.persistence.repository.MongoPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Query Handler — reads ONLY from MongoDB Read Model (never touches Event Store).
 * CQRS: query side is completely separated from command side.
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final MongoPaymentRepository mongoRepo;

    public Optional<PaymentResponse> findById(String paymentId) {
        return mongoRepo.findById(paymentId).map(this::toResponse);
    }

    public Optional<PaymentResponse> findByOrderId(String orderId) {
        return mongoRepo.findByOrderId(orderId).map(this::toResponse);
    }

    public List<PaymentResponse> findByCustomerId(String customerId) {
        return mongoRepo.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(PaymentDocument doc) {
        return PaymentResponse.builder()
                .id(doc.getId())
                .orderId(doc.getOrderId())
                .customerId(doc.getCustomerId())
                .amount(doc.getAmount())
                .currency(doc.getCurrency())
                .paymentMethodType(doc.getPaymentMethodType())
                .maskedAccount(doc.getMaskedAccount())
                .status(doc.getStatus())
                .failureReason(doc.getFailureReason())
                .transactionRef(doc.getTransactionRef())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
