package com.example.payment.application.command.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class InitiatePaymentCommand {

    @NotBlank
    String orderId;

    @NotBlank
    String customerId;

    @NotNull
    @DecimalMin("0.01")
    BigDecimal amount;

    @NotBlank
    String currency;

    @NotBlank
    String paymentMethodType;

    String maskedAccount;
}
