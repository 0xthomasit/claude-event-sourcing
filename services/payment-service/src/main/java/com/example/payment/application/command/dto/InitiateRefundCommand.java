package com.example.payment.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InitiateRefundCommand {

    @NotBlank
    String orderId;

    @NotBlank
    String reason;
}
