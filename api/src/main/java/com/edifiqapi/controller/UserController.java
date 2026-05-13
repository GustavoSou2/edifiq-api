package com.edifiqapi.controller;

import com.edifiqapi.domain.rbac.Invite;
import com.edifiqapi.domain.rbac.Role;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.service.InviteService;
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
    private final InviteService inviteService;

    public UserController(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            InviteService inviteService
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.inviteService = inviteService;
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
        if (request.active()    != null) user.setActive(request.active());
        if (request.fullName()  != null && !request.fullName().isBlank()) user.setFullName(request.fullName().trim());
        if (request.phone()     != null) user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        user.setUpdatedAt(Instant.now());
        return ApiResponse.of(UserResponse.from(userRepository.save(user)));
    }

    @GetMapping("/invites")
    public ApiResponse<List<InviteResponse>> listInvites(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(inviteService.listByTenant(tenantId).stream().map(InviteResponse::from).toList());
    }

    @PostMapping("/invites")
    public ApiResponse<InviteResponse> createInvite(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateInviteRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        String userId = JwtClaims.userId(jwt);
        Invite invite = inviteService.createInvite(tenantId, userId, request.email(), request.roleId(), request.expiresAt());
        return ApiResponse.of(InviteResponse.from(invite));
    }

    @PatchMapping("/invites/{inviteId}/cancel")
    public ApiResponse<InviteResponse> cancelInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable String inviteId) {
        String tenantId = JwtClaims.tenantId(jwt);
        Invite invite = inviteService.cancelInvite(tenantId, inviteId);
        return ApiResponse.of(InviteResponse.from(invite));
    }

    @PostMapping("/invites/confirm")
    public ApiResponse<UserResponse> confirmInvite(@Valid @RequestBody ConfirmInviteRequest request) {
        User user = inviteService.confirmInvite(request.token(), request.password(), request.fullName(), request.phone());
        return ApiResponse.of(UserResponse.from(user));
    }

    public record CreateUserRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record UpdateUserRequest(Boolean active, String fullName, String phone) {}

    public record CreateInviteRequest(@Email @NotBlank String email, @NotBlank String roleId, Instant expiresAt) {}
    public record ConfirmInviteRequest(@NotBlank String token, @NotBlank String password, String fullName, String phone) {}

    public record UserResponse(String id, String email, String fullName, String phone, boolean active, boolean emailVerified, Instant lastLoginAt, Instant createdAt) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.isActive(), user.isEmailVerified(), user.getLastLoginAt(), user.getCreatedAt());
        }
    }

    public record InviteResponse(
            String id,
            String tenantId,
            String email,
            String token,
            User invitedBy,
            Role role,
            Invite.Status status,
            Instant expiresAt,
            Instant acceptedAt,
            Instant createdAt
    ) {
        static InviteResponse from(Invite invite) {
            return new InviteResponse(
                    invite.getId(),
                    invite.getTenant().getId(),
                    invite.getEmail(),
                    invite.getToken(),
                    invite.getInvitedBy(),
                    invite.getRole(),
                    invite.getStatus(),
                    invite.getExpiresAt(),
                    invite.getAcceptedAt(),
                    invite.getCreatedAt()
            );
        }
    }
}
