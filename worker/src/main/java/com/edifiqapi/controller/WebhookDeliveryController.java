package com.edifiqapi.controller;

import com.edifiqapi.domain.webhook.WebhookDelivery;
import com.edifiqapi.repository.webhook.WebhookDeliveryRepository;
import com.edifiqapi.security.JwtClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/webhook-deliveries")
public class WebhookDeliveryController {
    private final WebhookDeliveryRepository webhookDeliveryRepository;

    public WebhookDeliveryController(WebhookDeliveryRepository webhookDeliveryRepository) {
        this.webhookDeliveryRepository = webhookDeliveryRepository;
    }

    @GetMapping
    public List<WebhookDeliveryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        long tenantId = JwtClaims.tenantId(jwt);
        return webhookDeliveryRepository.findAllByWebhook_Tenant_Id(tenantId).stream().map(WebhookDeliveryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public WebhookDeliveryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        long tenantId = JwtClaims.tenantId(jwt);
        WebhookDelivery delivery = webhookDeliveryRepository.findByIdAndWebhook_Tenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "webhook delivery not found"));
        return WebhookDeliveryResponse.from(delivery);
    }

    public record WebhookDeliveryResponse(
            Long id,
            Long webhookId,
            String event,
            Map<String, Object> payload,
            Integer statusCode,
            String response,
            Instant deliveredAt,
            Instant createdAt
    ) {
        static WebhookDeliveryResponse from(WebhookDelivery delivery) {
            return new WebhookDeliveryResponse(
                    delivery.getId(),
                    delivery.getWebhook().getId(),
                    delivery.getEvent(),
                    delivery.getPayload(),
                    delivery.getStatusCode(),
                    delivery.getResponse(),
                    delivery.getDeliveredAt(),
                    delivery.getCreatedAt()
            );
        }
    }
}

