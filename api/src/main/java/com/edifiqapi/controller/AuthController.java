package com.edifiqapi.controller;

import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        var result = authService.register(request.tenantName(), request.name(), request.email(), request.password());
        return new AuthResponse("Bearer", result.accessToken(), result.expiresAt(), result.user().getId(), result.tenantId(), result.user());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        AuthService.AuthResult result;
        if (request.tenantSlug() != null && !request.tenantSlug().isBlank()) {
            result = authService.loginWithTenant(request.tenantSlug(), request.email(), request.password());
        } else {
            result = authService.login(request.email(), request.password());
        }
        return new AuthResponse("Bearer", result.accessToken(), result.expiresAt(), result.user().getId(), result.tenantId(), result.user());
    }

    @GetMapping("/tenants-by-email")
    public List<TenantSummary> tenantsByEmail(@RequestParam @Email @NotBlank String email) {
        return authService.findTenantsByEmail(email);
    }

    public record RegisterRequest(
            @NotBlank String tenantName,
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record LoginRequest(
            String tenantSlug,           // opcional — só necessário se o email existir em múltiplos tenants
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String tokenType,
            String accessToken,
            Instant expiresAt,
            String userId,
            String tenantId,
            User user
    ) {}

    public record TenantSummary(
            String slug,
            String name
    ) {}
}



