package com.edifiqapi.repository.webhook;

import com.edifiqapi.domain.webhook.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "webhook-deliveries")
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {
    List<WebhookDelivery> findAllByWebhook_Tenant_Id(Long tenantId);

    Optional<WebhookDelivery> findByIdAndWebhook_Tenant_Id(Long id, Long tenantId);
}
