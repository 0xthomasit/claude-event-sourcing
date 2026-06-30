package com.example.inventory.application.command.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
public class ReplenishStockCommand {
    @NotBlank private final String productId;
    @Min(1)   private final int    quantity;
              private final String reference;
}
