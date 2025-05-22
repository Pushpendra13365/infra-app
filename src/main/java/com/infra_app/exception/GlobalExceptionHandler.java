package com.infra_app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                errors));
    }

    @ExceptionHandler({
            CustomUnauthorizedException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                null));
    }

    @ExceptionHandler({
            UserAlreadyExistsException.class,
            PasswordReuseException.class
    })
    public ResponseEntity<Map<String, Object>> handleConflictException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                null));
    }

    @ExceptionHandler({
            AccountLockedException.class,
            LockedException.class
    })
    public ResponseEntity<Map<String, Object>> handleLockedException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(createErrorResponse(
                HttpStatus.LOCKED,
                ex.getMessage(),
                null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                null));
    }

    private Map<String, Object> createErrorResponse(HttpStatus status, String message, Map<String, String> details) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);

        if (details != null) {
            response.put("details", details);
        }

        return response;
    }


}