package com.example.cart.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Result returned after cart checkout creates an order successfully.
 */
@Value
@Builder
public class CheckoutResponse {
    String orderId;
    String status;
}
