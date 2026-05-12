package com.edifiqapi.controller;

import com.edifiqapi.domain.rbac.UserRole;
import com.edifiqapi.repository.rbac.RoleRepository;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.repository.rbac.UserRoleRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/user-roles")
public class UserRoleController {
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserRoleController(UserRoleRepository userRoleRepository, UserRepository userRepository, RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public ApiResponse<List<UserRoleResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(userRoleRepository.findAllByUser_Tenant_Id(tenantId).stream().map(UserRoleResponse::from).toList());
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<UserRoleResponse>> findByUserId(@AuthenticationPrincipal Jwt jwt, @PathVariable String userId) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(userRoleRepository.findAllByUser_IdAndUser_Tenant_Id(userId, tenantId).stream().map(UserRoleResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<UserRoleResponse> grant(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GrantUserRoleRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        String grantedById = JwtClaims.userId(jwt);

        var user = userRepository.findByIdAndTenant_Id(request.userId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        var role = roleRepository.findByIdAndTenant_Id(request.roleId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "role not found"));
        var grantedBy = userRepository.findByIdAndTenant_Id(grantedById, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "grantedBy not found"));

        UserRole userRole = userRoleRepository
                .findByUser_IdAndRole_IdAndUser_Tenant_Id(request.userId(), request.roleId(), tenantId)
                .orElseGet(UserRole::new);

        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setGrantedBy(grantedBy);
        userRole.setGrantedAt(Instant.now());
        return ApiResponse.of(UserRoleResponse.from(userRoleRepository.save(userRole)));
    }

    @DeleteMapping("/{id}")
    public void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        UserRole userRole = userRoleRepository.findByIdAndUser_Tenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user role not found"));
        userRoleRepository.delete(userRole);
    }

    public record GrantUserRoleRequest(@NotNull String userId, @NotNull String roleId) {}

    public record UserRoleResponse(String id, String userId, String roleId, RoleData role, String grantedBy, Instant grantedAt) {
        static UserRoleResponse from(UserRole userRole) {
            return new UserRoleResponse(
                    userRole.getId(),
                    userRole.getUser().getId(),
                    userRole.getRole().getId(),
                    RoleData.from(userRole.getRole()),
                    userRole.getGrantedBy() != null ? userRole.getGrantedBy().getId() : null,
                    userRole.getGrantedAt()
            );
        }
    }

    public record RoleData(String id, String name, List<String> permissions, boolean system) {
        static RoleData from(com.edifiqapi.domain.rbac.Role role) {
            return new RoleData(role.getId(), role.getName(), role.getPermissions(), role.isSystem());
        }
    }
}
