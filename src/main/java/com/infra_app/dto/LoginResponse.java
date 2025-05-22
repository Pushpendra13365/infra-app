package com.infra_app.dto;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String data;
    private String message;
    private int status;
    private long timestamp;

    public LoginResponse(String token) {
        this.data = token;
        this.message = "success";
        this.status = 200;
        this.timestamp = System.currentTimeMillis();
    }
}

