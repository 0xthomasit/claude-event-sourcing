package com.example.inventory.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@lombok.AllArgsConstructor(onConstructor_ = {@com.fasterxml.jackson.annotation.JsonCreator})
public class AdjustStockCommand {
    @NotBlank private final String productId;
              private final int    delta;    // positive or negative
    @NotBlank private final String reason;
}
