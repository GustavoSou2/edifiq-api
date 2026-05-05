package com.edifiqapi.controller;

import com.edifiqapi.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(request.tenantSlug(), request.planName(), request.email(), request.password());
        return new AuthResponse("Bearer", result.accessToken(), result.expiresAt(), result.userId(), result.tenantId());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.tenantSlug(), request.email(), request.password());
        return new AuthResponse("Bearer", result.accessToken(), result.expiresAt(), result.userId(), result.tenantId());
    }

    public record RegisterRequest(
            @NotBlank String tenantSlug,
            @NotBlank String planName,
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record LoginRequest(
            @NotBlank String tenantSlug,
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String tokenType,
            String accessToken,
            Instant expiresAt,
            String userId,
            String tenantId
    ) {}
}



