package com.example.cart.application.dto;

import com.example.cart.domain.model.CartItem;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CartResponse {

    String         userId;
    List<CartItem> items;
    int            itemCount;
    BigDecimal     total;
    String         currency;
}
