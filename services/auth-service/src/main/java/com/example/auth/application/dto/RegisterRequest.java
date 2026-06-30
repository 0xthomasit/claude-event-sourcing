package com.example.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class RegisterRequest {

    @Email @NotBlank
    private final String email;

    @NotBlank @Size(min = 8, max = 100)
    private final String password;

    @NotBlank @Size(max = 100)
    private final String fullName;
}
