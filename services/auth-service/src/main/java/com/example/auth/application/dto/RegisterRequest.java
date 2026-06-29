package com.example.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class RegisterRequest {

    @Email @NotBlank
    private final String email;

    @NotBlank @Size(min = 8, max = 100)
    private final String password;

    @NotBlank @Size(max = 100)
    private final String fullName;
}