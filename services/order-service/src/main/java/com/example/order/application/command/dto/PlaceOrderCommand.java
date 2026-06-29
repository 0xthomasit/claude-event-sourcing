package com.example.order.application.command.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PlaceOrderCommand {

    @NotBlank
    private final String customerId;

    @NotEmpty @Valid
    private final List<OrderItemRequest> items;

    @Getter @Builder
    public static class OrderItemRequest {
        @NotBlank  private final String productId;
        private final String productName;
        @Min(1)    private final int quantity;
        @NotNull @DecimalMin("0.01") private final BigDecimal unitPrice;
        @NotBlank  private final String currency;
    }
}