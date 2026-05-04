package com.edifiqapi.service;

import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.tenant.Tenant;
import com.edifiqapi.repository.plan.PlanRepository;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            TenantRepository tenantRepository,
            PlanRepository planRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResult register(String tenantSlug, String planName, String email, String password) {
        if (tenantRepository.findBySlug(tenantSlug).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "tenantSlug already exists");
        }

        var plan = planRepository.findByName(planName)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "plan not found: " + planName));

        Tenant tenant = new Tenant();
        tenant.setPlan(plan);
        tenant.setSlug(tenantSlug);
        tenant.setStatus(Tenant.Status.trial);
        tenant.setTrialEndsAt(Instant.now().plusSeconds(14L * 24 * 3600));
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(true);
        user.setEmailVerified(false);
        user = userRepository.save(user);

        var token = jwtTokenService.createAccessToken(user);
        return new AuthResult(user.getId(), tenant.getId(), token.value(), token.expiresAt());
    }

    @Transactional(readOnly = true)
    public AuthResult login(String tenantSlug, String email, String password) {
        User user = userRepository.findByTenant_SlugAndEmail(tenantSlug, email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid credentials"));

        if (!user.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "user inactive");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
        }

        var token = jwtTokenService.createAccessToken(user);
        return new AuthResult(user.getId(), user.getTenant().getId(), token.value(), token.expiresAt());
    }

    public record AuthResult(Long userId, Long tenantId, String accessToken, Instant expiresAt) {}
}
