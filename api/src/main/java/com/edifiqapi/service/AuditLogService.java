package com.edifiqapi.service;

import com.edifiqapi.domain.audit.AuditLog;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.tenant.Tenant;
import com.edifiqapi.repository.audit.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(Tenant tenant, User user, String action, String entity, String entityId, Map<String, Object> payload) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTenant(tenant);
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setEntity(entity);
        auditLog.setEntityId(entityId);
        auditLog.setPayload(payload);
        auditLogRepository.save(auditLog);
    }
}



