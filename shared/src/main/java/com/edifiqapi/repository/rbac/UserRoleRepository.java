package com.edifiqapi.repository.rbac;

import com.edifiqapi.domain.rbac.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "user-roles")
public interface UserRoleRepository extends JpaRepository<UserRole, String> {
    List<UserRole> findAllByUser_Tenant_Id(String tenantId);

    Optional<UserRole> findByIdAndUser_Tenant_Id(String id, String tenantId);
}

