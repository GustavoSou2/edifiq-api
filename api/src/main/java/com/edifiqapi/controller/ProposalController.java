package com.edifiqapi.controller;

import com.edifiqapi.domain.order.OrderDistribution;
import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.proposal.ProposalItem;
import com.edifiqapi.repository.order.OrderDistributionRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.service.OrderFlowService;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class ProposalController {
    private final OrderFlowService orderFlowService;
    private final OrderDistributionRepository orderDistributionRepository;

    public ProposalController(
            OrderFlowService orderFlowService,
            OrderDistributionRepository orderDistributionRepository
    ) {
        this.orderFlowService = orderFlowService;
        this.orderDistributionRepository = orderDistributionRepository;
    }

    @GetMapping("/distributions/received")
    public ApiResponse<List<DistributionResponse>> received(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(
                orderDistributionRepository.findAllBySupplierTenant_Id(tenantId).stream()
                        .map(DistributionResponse::from)
                        .toList()
        );
    }

    @PostMapping("/distributions/{distributionId}/proposals")
    public ApiResponse<ProposalResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String distributionId,
            @Valid @RequestBody SubmitProposalRequest request
    ) {
        String tenantId = JwtClaims.tenantId(jwt);
        Proposal proposal = orderFlowService.submitProposal(
                tenantId,
                distributionId,
                request.status(),
                request.deliveryEtaHours(),
                request.proposedDeliveryAt(),
                request.message(),
                request.items().stream()
                        .map(i -> new OrderFlowService.CreateProposalItem(i.orderItemId(), i.unitPrice(), i.totalPrice(), i.availability()))
                        .toList()
        );
        return ApiResponse.of(ProposalResponse.from(proposal));
    }

    public record SubmitProposalRequest(
            @NotNull Proposal.Status status,
            Integer deliveryEtaHours,
            /** Data/hora concreta que o fornecedor confirma conseguir entregar. */
            Instant proposedDeliveryAt,
            String message,
            @NotNull List<SubmitProposalItemRequest> items
    ) {}

    public record SubmitProposalItemRequest(
            @NotNull String orderItemId,
            @NotNull BigDecimal unitPrice,
            @NotNull BigDecimal totalPrice,
            @NotNull ProposalItem.Availability availability
    ) {}

    public record ProposalResponse(
            String id,
            String distributionId,
            Proposal.Status status,
            BigDecimal totalPrice,
            Integer deliveryEtaHours,
            Instant proposedDeliveryAt,
            String message
    ) {
        static ProposalResponse from(Proposal proposal) {
            return new ProposalResponse(
                    proposal.getId(),
                    proposal.getOrderDistribution().getId(),
                    proposal.getStatus(),
                    proposal.getTotalPrice(),
                    proposal.getDeliveryEtaHours(),
                    proposal.getProposedDeliveryAt(),
                    proposal.getMessage()
            );
        }
    }

    public record DistributionResponse(
            String id,
            String orderId,
            String supplierId,
            String buyerTenantId,
            String supplierTenantId,
            OrderDistribution.Status status,
            Instant distributedAt
    ) {
        static DistributionResponse from(OrderDistribution distribution) {
            return new DistributionResponse(
                    distribution.getId(),
                    distribution.getOrder().getId(),
                    distribution.getSupplier().getId(),
                    distribution.getBuyerTenant().getId(),
                    distribution.getSupplierTenant().getId(),
                    distribution.getStatus(),
                    distribution.getDistributedAt()
            );
        }
    }
}
