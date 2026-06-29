package com.example.inventory.application.command.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReleaseStockCommand {
    private final String productId;
    private final String orderId;
    private final int    quantity;
}