package com.infra_app.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private T data;
    private String message;
    private String messageCode;
    private int status;
    private long timestamp;

    public static <T> ApiResponse<T> success(String message, String messageCode, T data, int status) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .messageCode(messageCode)
                .status(status)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String messageCode, int status) {
        return ApiResponse.<T>builder()
                .data(null)
                .message(message)
                .messageCode(messageCode)
                .status(status)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
}
