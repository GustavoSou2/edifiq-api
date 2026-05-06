package com.edifiqapi.service;

import com.edifiqapi.controller.AuthController;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.tenant.Tenant;
import com.edifiqapi.repository.plan.PlanRepository;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

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
    public AuthResult register(String tenantName, String name, String email, String password) {
        String tenantSlug = toSlug(tenantName);

        if (tenantRepository.findBySlug(tenantSlug).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "tenantSlug already exists");
        }

        var plan = planRepository.findByName("free")
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "plan not found: " + "free"));

        Tenant tenant = new Tenant();
        tenant.setPlan(plan);
        tenant.setSlug(tenantSlug);
        tenant.setName(tenantName);
        tenant.setStatus(Tenant.Status.trial);
        tenant.setTrialEndsAt(Instant.now().plusSeconds(14L * 24 * 3600));
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(email);
        user.setFullName(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(true);
        user.setEmailVerified(false);
        user = userRepository.save(user);

        var token = jwtTokenService.createAccessToken(user);
        return new AuthResult(user, tenant.getId(), token.value(), token.expiresAt());
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        List<User> matches = userRepository.findAllByEmail(email);

        if (matches.isEmpty()) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
        }

        if (matches.size() > 1) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "multiple accounts found for this email, please provide tenantSlug");
        }

        User user = matches.getFirst();

        if (!user.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "user inactive");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
        }

        var token = jwtTokenService.createAccessToken(user);
        return new AuthResult(user, user.getTenant().getId(), token.value(), token.expiresAt());
    }

    @Transactional(readOnly = true)
    public AuthResult loginWithTenant(String tenantSlug, String email, String password) {
        User user = userRepository.findByTenant_SlugAndEmail(tenantSlug, email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid credentials"));

        if (!user.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "user inactive");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
        }

        var token = jwtTokenService.createAccessToken(user);
        return new AuthResult(user, user.getTenant().getId(), token.value(), token.expiresAt());
    }

    @Transactional(readOnly = true)
    public List<AuthController.TenantSummary> findTenantsByEmail(String email) {
        return userRepository.findAllByEmail(email).stream()
                .map(u -> new AuthController.TenantSummary(
                        u.getTenant().getSlug(),
                        u.getTenant().getName()
                ))
                .toList();
    }

    public record AuthResult(User user, String tenantId, String accessToken, Instant expiresAt) {}

    public static String toSlug(String input) {
        if (input == null) return null;

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String slug = normalized.toLowerCase();

        slug = slug.replaceAll("[^a-z0-9]+", "-");

        slug = slug.replaceAll("-{2,}", "-");

        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }
}


