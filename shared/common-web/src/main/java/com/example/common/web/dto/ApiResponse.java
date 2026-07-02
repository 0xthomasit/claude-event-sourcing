package com.example.common.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    // ─── Factory methods ──────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data, int status) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, 200);
    }

    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .status(0)
                .message(message)
                .build();
    }

    public static ApiResponse<Void> error(String message, int status) {
        return ApiResponse.<Void>builder()
                .success(false)
                .status(status)
                .message(message)
                .build();
    }
}
