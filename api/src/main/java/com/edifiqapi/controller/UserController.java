package com.edifiqapi.controller;

import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/users")
public class UserController {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(userRepository.findAllByTenant_Id(tenantId).stream().map(UserResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(userRepository.findByIdAndTenant_Id(id, tenantId).map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found")));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setEmailVerified(false);
        user.setLastLoginAt(null);
        return ApiResponse.of(UserResponse.from(userRepository.save(user)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        User user = userRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        if (request.active() != null) {
            user.setActive(request.active());
        }
        return ApiResponse.of(UserResponse.from(userRepository.save(user)));
    }

    public record CreateUserRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record UpdateUserRequest(Boolean active) {}

    public record UserResponse(String id, String email, boolean active, boolean emailVerified, Instant lastLoginAt) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.isActive(), user.isEmailVerified(), user.getLastLoginAt());
        }
    }
}
