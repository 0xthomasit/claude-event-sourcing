package com.example.common.events.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemDto {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final String currency;
}