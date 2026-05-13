package com.edifiqapi.repository.rbac;

import com.edifiqapi.domain.rbac.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "invites")
public interface InviteRepository extends JpaRepository<Invite, String> {
    List<Invite> findAllByTenant_IdOrderByCreatedAtDesc(String tenantId);

    Optional<Invite> findByIdAndTenant_Id(String id, String tenantId);

    Optional<Invite> findByTokenAndStatus(String token, Invite.Status status);

    boolean existsByTenant_IdAndEmailAndStatus(String tenantId, String email, Invite.Status status);

    @Modifying
    @Query("""
            update Invite i
            set i.status = :expired
            where i.status = :pending and i.expiresAt <= :now
            """)
    int markExpiredInvites(
            @Param("pending") Invite.Status pending,
            @Param("expired") Invite.Status expired,
            @Param("now") Instant now
    );
}
