//package com.infra_app.util;
//
//import com.infra_app.dto.ApiResponse;
//public class ResponseBuilder {
//
//    public static <T> ApiResponse<T> success(T data, String message, String messageCode, Long userId) {
//        return new ApiResponse<>(
//                data,
//                message,
//                messageCode,
//                200,
//                userId,
//                System.currentTimeMillis()
//        );
//    }
//
//    public static <T> ApiResponse<T> error(String message, String messageCode, int status) {
//        return ApiResponse.<T>builder()
//                .data(null)
//                .message(message)
//                .messageCode(messageCode)
//                .status(status)
//                .timestamp(System.currentTimeMillis())
//                .build();
//    }
//}
//
