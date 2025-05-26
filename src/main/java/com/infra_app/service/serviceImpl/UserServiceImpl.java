package com.infra_app.service.serviceImpl;

import com.infra_app.dto.*;
import com.infra_app.exception.*;
import com.infra_app.model.*;
import com.infra_app.repository.*;
import com.infra_app.security.JwtUtil;
import com.infra_app.service.LoginAttemptService;
import com.infra_app.service.UserService;
import com.infra_app.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountLockedException;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final UserTokenRepository tokenRepo;
    private final PasswordHistoryRepository passwordHistoryRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) throws AccountLockedException {
        String ipAddress = getClientIP();
        if (loginAttemptService.isBlocked(ipAddress)) {
            throw new AccountLockedException("Account temporarily locked due to multiple failed attempts");
        }

        try {
            User user = userRepo.findByUsername(request.getUserName())
                    .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                loginAttemptService.loginFailed(ipAddress);
                throw new BadCredentialsException("Invalid username or password");
            }

//            if (user.isLocked()) {
//                throw new AccountLockedException("Account is locked. Please contact administrator");
//            }

            loginAttemptService.loginSucceeded(ipAddress);

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(user.getRole().name()));

            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), authorities);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            tokenRepo.invalidateAllUserTokens(user.getId());
            tokenRepo.saveAll(List.of(
                    UserToken.builder()
                            .user(user)
                            .token(accessToken)
                            .tokenType(TokenType.ACCESS)
                            .expiryTime(LocalDateTime.now().plusHours(3))
                            .build(),
                    UserToken.builder()
                            .user(user)
                            .token(refreshToken)
                            .tokenType(TokenType.REFRESH)
                            .expiryTime(LocalDateTime.now().plusDays(7))
                            .build()
            ));
            CookieUtil.addJwtCookie( response, accessToken);

            log.info("User '{}' logged in successfully", user.getUsername());
            return new LoginResponse(accessToken);
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getUserName());
            throw new CustomUnauthorizedException("Invalid username or password");
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomUnauthorizedException("Invalid refresh token");
        }

        UserToken storedToken = tokenRepo.findByTokenAndTokenTypeAndBlacklistedFalse(refreshToken, TokenType.REFRESH)
                .orElseThrow(() -> new CustomUnauthorizedException("Invalid refresh token"));

        if (storedToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new CustomUnauthorizedException("Refresh token expired");
        }

        User user = storedToken.getUser();
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().name()));

        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), authorities);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        tokenRepo.invalidateToken(refreshToken);

        tokenRepo.saveAll(List.of(
                UserToken.builder()
                        .user(user)
                        .token(newAccessToken)
                        .tokenType(TokenType.ACCESS)
                        .expiryTime(LocalDateTime.now().plusHours(3))
                        .build(),
                UserToken.builder()
                        .user(user)
                        .token(newRefreshToken)
                        .tokenType(TokenType.REFRESH)
                        .expiryTime(LocalDateTime.now().plusDays(7))
                        .build()
        ));

        log.info("User '{}' refreshed tokens", user.getUsername());
        return new AuthResponse(newAccessToken, newRefreshToken, jwtUtil.getAccessTokenExpirationMs());
    }

    @Override
    @Transactional
    public void logout(String token, HttpServletResponse response) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }

        String token1 = token.substring(7);

        if (token1.isEmpty()) {
            throw new CustomUnauthorizedException("Token must not be empty");
        }

        int updated = tokenRepo.invalidateToken(token1);

        if (updated == 0) {
            log.warn("Token not found or already invalidated: {}", token);
            throw new CustomUnauthorizedException("Token not found or already invalidated");
        }

        log.info("Successfully logged out, token invalidated: {}", token);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Invalid role specified: " + request.getRole());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
                //.locked(false)


        userRepo.save(user);

        passwordHistoryRepo.save(PasswordHistory.builder()
                .user(user)
                .passwordHash(user.getPassword())
                .changedAt(LocalDateTime.now())
                .build());

        log.info("New user registered: {}", user.getUsername());
        return new RegisterResponse("User registered successfully", user.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        passwordHistoryRepo.findTop5ByUserOrderByChangedAtDesc(user)
                .stream()
                .filter(ph -> passwordEncoder.matches(request.getNewPassword(), ph.getPasswordHash()))
                .findAny()
                .ifPresent(ph -> {
                    throw new PasswordReuseException("New password cannot be the same as one of your last 5 passwords");
                });

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);

        passwordHistoryRepo.save(PasswordHistory.builder()
                .user(user)
                .passwordHash(user.getPassword())
                .changedAt(LocalDateTime.now())
                .build());

        tokenRepo.invalidateAllUserTokens(user.getId());

        log.info("Password changed for user: {}", user.getUsername());
    }

    private String getClientIP() {
        return "unknown";
    }
}