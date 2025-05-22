package com.infra_app.service;

import com.infra_app.dto.*;
import jakarta.validation.Valid;

import javax.security.auth.login.AccountLockedException;

public interface UserService {
    LoginResponse login(LoginRequest request) throws AccountLockedException;
    void logout(String token);
    RegisterResponse register(RegisterRequest request);

    AuthResponse refreshToken(@Valid RefreshTokenRequest request);

    void changePassword(@Valid ChangePasswordRequest request, String token);
}
