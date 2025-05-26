package com.infra_app.service;

import com.infra_app.dto.*;
import javax.security.auth.login.AccountLockedException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

public interface UserService {
    LoginResponse login(LoginRequest request, HttpServletResponse response) throws AccountLockedException;
    void logout(String token, HttpServletResponse response);
    RegisterResponse register(RegisterRequest request);

    AuthResponse refreshToken(@Valid RefreshTokenRequest request);

    void changePassword(@Valid ChangePasswordRequest request, String token);
}
