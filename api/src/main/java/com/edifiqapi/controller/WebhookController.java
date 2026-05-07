package com.edifiqapi.controller;

import com.edifiqapi.domain.webhook.Webhook;
import com.edifiqapi.repository.tenant.TenantRepository;
import com.edifiqapi.repository.webhook.WebhookRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {
    private final WebhookRepository webhookRepository;
    private final TenantRepository tenantRepository;

    public WebhookController(WebhookRepository webhookRepository, TenantRepository tenantRepository) {
        this.webhookRepository = webhookRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public ApiResponse<List<WebhookResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(webhookRepository.findAllByTenant_Id(tenantId).stream().map(WebhookResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<WebhookResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(webhookRepository.findByIdAndTenant_Id(id, tenantId).map(WebhookResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "webhook not found")));
    }

    @PostMapping
    public ApiResponse<WebhookResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertWebhookRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));

        Webhook webhook = new Webhook();
        webhook.setTenant(tenant);
        webhook.setUrl(request.url());
        webhook.setEvents(request.events());
        webhook.setSecret(request.secret());
        webhook.setActive(request.active());
        return ApiResponse.of(WebhookResponse.from(webhookRepository.save(webhook)));
    }

    @PutMapping("/{id}")
    public ApiResponse<WebhookResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertWebhookRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        Webhook webhook = webhookRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "webhook not found"));

        webhook.setUrl(request.url());
        webhook.setEvents(request.events());
        webhook.setSecret(request.secret());
        webhook.setActive(request.active());
        return ApiResponse.of(WebhookResponse.from(webhookRepository.save(webhook)));
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        Webhook webhook = webhookRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "webhook not found"));
        webhookRepository.delete(webhook);
    }

    public record UpsertWebhookRequest(
            @NotBlank String url,
            @NotNull List<String> events,
            @NotBlank String secret,
            boolean active
    ) {}

    public record WebhookResponse(String id, String url, List<String> events, boolean active) {
        static WebhookResponse from(Webhook webhook) {
            return new WebhookResponse(webhook.getId(), webhook.getUrl(), webhook.getEvents(), webhook.isActive());
        }
    }
}
