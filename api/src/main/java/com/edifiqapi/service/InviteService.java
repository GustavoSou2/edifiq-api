package com.edifiqapi.service;

import com.edifiqapi.domain.rbac.Invite;
import com.edifiqapi.domain.rbac.Role;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.rbac.UserRole;
import com.edifiqapi.repository.rbac.InviteRepository;
import com.edifiqapi.repository.rbac.RoleRepository;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.repository.rbac.UserRoleRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class InviteService {
    private static final Logger log = LoggerFactory.getLogger(InviteService.class);

    private final InviteRepository inviteRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public InviteService(
            InviteRepository inviteRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.inviteRepository = inviteRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Invite createInvite(String tenantId, String invitedByUserId, String email, String roleId, Instant expiresAt) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));

        User invitedBy = userRepository.findByIdAndTenant_Id(invitedByUserId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "inviter user not found"));

        if (inviteRepository.existsByTenant_IdAndEmailAndStatus(tenantId, email, Invite.Status.PENDING)) {
            throw new ResponseStatusException(BAD_REQUEST, "pending invite already exists for this email");
        }

        Role role = roleRepository.findByIdAndTenant_Id(roleId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "role not found in tenant"));

        Instant resolvedExpiresAt = expiresAt != null ? expiresAt : Instant.now().plus(7, ChronoUnit.DAYS);
        if (!resolvedExpiresAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "expiresAt must be in the future");
        }

        Invite invite = new Invite();
        invite.setTenant(tenantRepository.getReferenceById(tenantId));
        invite.setEmail(email.trim().toLowerCase());
        invite.setToken(UUID.randomUUID().toString());
        invite.setInvitedBy(invitedBy);
        invite.setRole(role);
        invite.setStatus(Invite.Status.PENDING);
        invite.setExpiresAt(resolvedExpiresAt);

        return inviteRepository.save(invite);
    }

    @Transactional(readOnly = true)
    public List<Invite> listByTenant(String tenantId) {
        return inviteRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public Invite cancelInvite(String tenantId, String inviteId) {
        Invite invite = inviteRepository.findByIdAndTenant_Id(inviteId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "invite not found"));
        invite.setStatus(Invite.Status.CANCELED);
        return inviteRepository.save(invite);
    }

    @Transactional
    public User confirmInvite(String token, String password, String fullName, String phone) {
        Invite invite = inviteRepository.findByTokenAndStatus(token, Invite.Status.PENDING)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "invite not found or already used"));

        if (!invite.getExpiresAt().isAfter(Instant.now())) {
            invite.setStatus(Invite.Status.EXPIRED);
            inviteRepository.save(invite);
            throw new ResponseStatusException(BAD_REQUEST, "invite expired");
        }

        String tenantId = invite.getTenant().getId();
        String email = invite.getEmail().trim().toLowerCase();
        userRepository.findByTenant_IdAndEmail(tenantId, email).ifPresent(existing -> {
            throw new ResponseStatusException(BAD_REQUEST, "user already exists for this invite email");
        });

        User user = new User();
        user.setTenant(invite.getTenant());
        user.setEmail(email);
        user.setFullName((fullName == null || fullName.isBlank()) ? email : fullName.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setActive(true);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(invite.getRole());
        userRole.setGrantedBy(invite.getInvitedBy());
        userRoleRepository.save(userRole);

        inviteRepository.delete(invite);
        return user;
    }

    @Transactional
    @Scheduled(cron = "${edifiq.invites.cleanup-cron:0 */15 * * * *}")
    public void cleanupExpiredInvites() {
        int updated = inviteRepository.markExpiredInvites(Invite.Status.PENDING, Invite.Status.EXPIRED, Instant.now());
        if (updated > 0) {
            log.info("Invite cleanup marked {} invite(s) as EXPIRED", updated);
        }
    }
}
