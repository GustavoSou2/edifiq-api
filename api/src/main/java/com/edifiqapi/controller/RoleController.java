package com.edifiqapi.controller;

import com.edifiqapi.domain.rbac.Role;
import com.edifiqapi.repository.rbac.RoleRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/roles")
public class RoleController {
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    public RoleController(RoleRepository roleRepository, TenantRepository tenantRepository) {
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(roleRepository.findAllByTenant_Id(tenantId).stream().map(RoleResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(roleRepository.findByIdAndTenant_Id(id, tenantId).map(RoleResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "role not found")));
    }

    @PostMapping
    public ApiResponse<RoleResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertRoleRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));

        Role role = new Role();
        role.setTenant(tenant);
        role.setName(request.name());
        role.setPermissions(request.permissions());
        role.setSystem(false);
        return ApiResponse.of(RoleResponse.from(roleRepository.save(role)));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertRoleRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        Role role = roleRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "role not found"));
        if (role.isSystem()) {
            throw new ResponseStatusException(BAD_REQUEST, "system role cannot be updated");
        }
        role.setName(request.name());
        role.setPermissions(request.permissions());
        return ApiResponse.of(RoleResponse.from(roleRepository.save(role)));
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        Role role = roleRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "role not found"));
        if (role.isSystem()) {
            throw new ResponseStatusException(BAD_REQUEST, "system role cannot be deleted");
        }
        roleRepository.delete(role);
    }

    public record UpsertRoleRequest(@NotBlank String name, @NotNull List<String> permissions) {}

    public record RoleResponse(String id, String name, List<String> permissions, boolean system) {
        static RoleResponse from(Role role) {
            return new RoleResponse(role.getId(), role.getName(), role.getPermissions(), role.isSystem());
        }
    }
}
