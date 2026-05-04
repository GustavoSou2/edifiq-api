package com.edifiqapi.controller;

import com.edifiqapi.repository.audit.AuditLogRepository;
import com.edifiqapi.security.JwtClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/audit-logs")
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<AuditLogResponse> list(@AuthenticationPrincipal Jwt jwt) {
        long tenantId = JwtClaims.tenantId(jwt);
        return auditLogRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId).stream()
                .map(a -> new AuditLogResponse(
                        a.getId(),
                        a.getAction(),
                        a.getEntity(),
                        a.getEntityId(),
                        a.getPayload(),
                        a.getCreatedAt()
                ))
                .toList();
    }

    public record AuditLogResponse(
            Long id,
            String action,
            String entity,
            Long entityId,
            Map<String, Object> payload,
            Instant createdAt
    ) {}
}

