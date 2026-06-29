package com.example.user.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ProfileResponse {

    UUID          id;
    String        authId;
    String        fullName;
    String        email;
    String        phone;
    String        avatarUrl;
    String        status;
    List<AddressResponse> addresses;
    Instant       createdAt;
    Instant       updatedAt;
}
