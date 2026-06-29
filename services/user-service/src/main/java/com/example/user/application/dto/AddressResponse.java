package com.example.user.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AddressResponse {

    UUID    id;
    String  label;
    String  recipientName;
    String  phone;
    String  street;
    String  ward;
    String  district;
    String  province;
    boolean isDefault;
}
