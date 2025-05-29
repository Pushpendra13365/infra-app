package com.infra_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long userId;
    private String message;
    private int status;
    private long timestamp;
    private String messageCode;

    public LoginResponse(String token, Long id) {
        this.userId = id;
        this.token = token;
        this.message = "Login successful";
        this.status = 200;
        this.timestamp = System.currentTimeMillis();
        this.messageCode = "LOGIN_SUCCESS";
    }

}

