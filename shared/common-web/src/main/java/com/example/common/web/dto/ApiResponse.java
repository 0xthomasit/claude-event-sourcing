package com.example.common.web.dto;

import tools.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final int status;
    private final String message;
    private final T data;
    
    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data, int status) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message("Success")
                .data(data)
                .build();
    }
}
