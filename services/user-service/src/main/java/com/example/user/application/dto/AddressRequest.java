package com.example.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddressRequest {

    String  label;

    @NotBlank
    String recipientName;

    @NotBlank
    String phone;

    @NotBlank
    String street;

    String  ward;

    @NotBlank
    String district;

    @NotBlank
    String province;

    boolean isDefault;
}
