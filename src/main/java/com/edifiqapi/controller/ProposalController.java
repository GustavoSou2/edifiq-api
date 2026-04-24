package com.edifiqapi.controller;

import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.proposal.ProposalItem;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.service.OrderFlowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class ProposalController {
    private final OrderFlowService orderFlowService;

    public ProposalController(OrderFlowService orderFlowService) {
        this.orderFlowService = orderFlowService;
    }

    @PostMapping("/distributions/{distributionId}/proposals")
    public ProposalResponse submit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long distributionId, @Valid @RequestBody SubmitProposalRequest request) {
        long tenantId = JwtClaims.tenantId(jwt);
        Proposal proposal = orderFlowService.submitProposal(
                tenantId,
                distributionId,
                request.status(),
                request.deliveryEtaHours(),
                request.message(),
                request.items().stream()
                        .map(i -> new OrderFlowService.CreateProposalItem(i.orderItemId(), i.unitPrice(), i.totalPrice(), i.availability()))
                        .toList()
        );
        return ProposalResponse.from(proposal);
    }

    public record SubmitProposalRequest(
            @NotNull Proposal.Status status,
            Integer deliveryEtaHours,
            String message,
            @NotNull List<SubmitProposalItemRequest> items
    ) {}

    public record SubmitProposalItemRequest(
            @NotNull Long orderItemId,
            @NotNull BigDecimal unitPrice,
            @NotNull BigDecimal totalPrice,
            @NotNull ProposalItem.Availability availability
    ) {}

    public record ProposalResponse(Long id, Long distributionId, Proposal.Status status) {
        static ProposalResponse from(Proposal proposal) {
            return new ProposalResponse(proposal.getId(), proposal.getOrderDistribution().getId(), proposal.getStatus());
        }
    }
}

