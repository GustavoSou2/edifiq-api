package com.edifiqapi.repository.webhook;

import com.edifiqapi.domain.webhook.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "webhooks")
public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    List<Webhook> findAllByTenant_Id(Long tenantId);

    Optional<Webhook> findByIdAndTenant_Id(Long id, Long tenantId);
}
