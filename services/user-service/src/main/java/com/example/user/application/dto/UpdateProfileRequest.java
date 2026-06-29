package com.example.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UpdateProfileRequest {

    @NotBlank
    String fullName;

    String phone;
    String avatarUrl;
}
