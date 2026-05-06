package com.edifiqapi.repository.rbac;

import com.edifiqapi.domain.rbac.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "users")
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByTenant_SlugAndEmail(String tenantSlug, String email);

    Optional<User> findByEmail(String email);

    List<User> findAllByEmail(String email);

    List<User> findAllByTenant_Id(String tenantId);

    Optional<User> findByIdAndTenant_Id(String id, String tenantId);
}

