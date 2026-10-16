package com.studbuddy.backend.controller;

import com.studbuddy.backend.dto.AuthResponse;
import com.studbuddy.backend.dto.LoginRequest;
import com.studbuddy.backend.dto.RegisterRequest;
import com.studbuddy.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController.java — The Authentication Endpoints
 *
 * Two endpoints:
 *   POST /api/auth/register  → create a new account
 *   POST /api/auth/login     → verify credentials, get JWT
 *
 * Both are PUBLIC (no token required) — configured in SecurityConfig.
 *
 * This controller is intentionally thin:
 *   - Receives the request
 *   - Validates the input (@Valid)
 *   - Delegates to AuthService
 *   - Returns the HTTP response
 *
 * ALL business logic (hashing, token generation, duplicate check) is in AuthService.
 */
@RestController
@RequestMapping("/api/auth")
// ↑ All endpoints: /api/auth/register and /api/auth/login
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ================================================================
    // REGISTER — POST /api/auth/register
    // ================================================================
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // ↑ @Valid → validates @NotBlank, @Email, @Size on RegisterRequest
        // ↑ @RequestBody → reads JSON from request body and converts to RegisterRequest
        //   If validation fails → GlobalExceptionHandler returns 400 before this runs

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        // ↑ 201 Created — standard for "a new resource was successfully created"
    }

    // ================================================================
    // LOGIN — POST /api/auth/login
    // ================================================================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
        // ↑ 200 OK — login doesn't create a new resource, it issues a token
    }
}
