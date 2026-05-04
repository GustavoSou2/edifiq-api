package com.edifiqapi.repository.audit;

import com.edifiqapi.domain.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(path = "audit-logs")
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);
}
