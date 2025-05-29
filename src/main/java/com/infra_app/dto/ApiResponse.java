package com.infra_app.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
   // private T data;
    private Long userId;
    private String message;
    private String messageCode;
    private int status;
    private long timestamp;

    public static <T> ApiResponse<T> success(String message, String messageCode, int status, Long userId) {
        return ApiResponse.<T>builder()
                .message(message)
                .messageCode(messageCode)
                .status(status)
                .timestamp(Instant.now().toEpochMilli())
                .userId(userId)
                .build();
                //.data(data)

    }

    public static <T> ApiResponse<T> error(String message, String messageCode, int status) {
        return ApiResponse.<T>builder()
                .message(message)
                .messageCode(messageCode)
                .status(status)
                .timestamp(Instant.now().toEpochMilli())
                .build();
                //.data(null)

    }
}
