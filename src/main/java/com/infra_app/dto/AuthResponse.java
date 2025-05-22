package com.infra_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;

    public AuthResponse(String accessToken, String refreshToken, Object accessTokenExpirationMs) {
    }

    //public AuthResponse(String accessToken, String refreshToken, Object accessTokenExpirationMs) {

}
