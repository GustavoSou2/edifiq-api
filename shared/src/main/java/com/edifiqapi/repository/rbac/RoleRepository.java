package com.edifiqapi.repository.rbac;

import com.edifiqapi.domain.rbac.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "roles")
public interface RoleRepository extends JpaRepository<Role, String> {
    List<Role> findAllByTenant_Id(String tenantId);

    Optional<Role> findByIdAndTenant_Id(String id, String tenantId);
}

