package com.edifiqapi.controller;

import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.web.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/me")
public class MeController {
    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        String userId = JwtClaims.userId(jwt);

        var user = userRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        return ApiResponse.of(new MeResponse(user, user.getEmail(), user.getTenant().getId(), user.getTenant().getSlug()));
    }

    public record MeResponse(User user, String email, String tenantId, String tenantSlug) {}
}
