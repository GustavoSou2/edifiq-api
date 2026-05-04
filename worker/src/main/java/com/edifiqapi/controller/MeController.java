package com.edifiqapi.controller;

import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.security.JwtClaims;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        long tenantId = JwtClaims.tenantId(jwt);
        long userId = JwtClaims.userId(jwt);

        var user = userRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        return new MeResponse(user.getId(), user.getEmail(), user.getTenant().getId(), user.getTenant().getSlug());
    }

    public record MeResponse(Long userId, String email, Long tenantId, String tenantSlug) {}
}

