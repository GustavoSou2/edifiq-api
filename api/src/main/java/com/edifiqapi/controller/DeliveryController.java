package com.edifiqapi.controller;

import com.edifiqapi.domain.delivery.Delivery;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.service.OrderFlowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/v1/deliveries")
public class DeliveryController {
    private final OrderFlowService orderFlowService;

    public DeliveryController(OrderFlowService orderFlowService) {
        this.orderFlowService = orderFlowService;
    }

    @PatchMapping("/{id}")
    public DeliveryResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody UpdateDeliveryRequest request) {
        long tenantId = JwtClaims.tenantId(jwt);
        Delivery delivery = orderFlowService.updateDeliveryStatus(tenantId, id, request.status(), request.trackingCode(), request.proofUrl());
        return DeliveryResponse.from(delivery);
    }

    public record UpdateDeliveryRequest(
            @NotNull Delivery.Status status,
            String trackingCode,
            String proofUrl
    ) {}

    public record DeliveryResponse(
            Long id,
            Delivery.Status status,
            String trackingCode,
            Instant scheduledAt,
            Instant dispatchedAt,
            Instant deliveredAt,
            String proofUrl
    ) {
        static DeliveryResponse from(Delivery delivery) {
            return new DeliveryResponse(
                    delivery.getId(),
                    delivery.getStatus(),
                    delivery.getTrackingCode(),
                    delivery.getScheduledAt(),
                    delivery.getDispatchedAt(),
                    delivery.getDeliveredAt(),
                    delivery.getProofUrl()
            );
        }
    }
}

