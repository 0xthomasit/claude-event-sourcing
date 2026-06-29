package com.example.payment.application.query.dto;

import com.example.payment.domain.model.PaymentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PaymentResponse {

    String        id;
    String        orderId;
    String        customerId;
    BigDecimal    amount;
    String        currency;
    String        paymentMethodType;
    String        maskedAccount;
    PaymentStatus status;
    String        failureReason;
    String        transactionRef;
    Instant       createdAt;
    Instant       updatedAt;
}
