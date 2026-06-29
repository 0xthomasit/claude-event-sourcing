package com.example.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateProfileRequest {

    @NotBlank
    String authId;

    @NotBlank
    String fullName;

    @NotBlank
    @Email
    String email;

    String phone;
    String avatarUrl;
}
