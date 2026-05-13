package com.edifiqapi.controller;

import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.supplier.Supplier;
import com.edifiqapi.repository.proposal.ProposalRepository;
import com.edifiqapi.repository.supplier.SupplierRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/suppliers")
public class SupplierController {
    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;
    private final ProposalRepository proposalRepository;

    public SupplierController(
            SupplierRepository supplierRepository,
            TenantRepository tenantRepository,
            ProposalRepository proposalRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.tenantRepository = tenantRepository;
        this.proposalRepository = proposalRepository;
    }

    @GetMapping
    public ApiResponse<List<SupplierResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(supplierRepository.findAllByTenant_Id(tenantId).stream().map(SupplierResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(supplierRepository.findByIdAndTenant_Id(id, tenantId).map(SupplierResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "supplier not found")));
    }

    @GetMapping("/{id}/proposals")
    public ApiResponse<List<SupplierProposalResponse>> listProposals(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        supplierRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "supplier not found"));

        return ApiResponse.of(
                proposalRepository.findAllByOrderDistribution_Supplier_IdAndOrderDistribution_SupplierTenant_Id(id, tenantId)
                        .stream()
                        .map(SupplierProposalResponse::from)
                        .toList()
        );
    }

    @PostMapping
    public ApiResponse<SupplierResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertSupplierRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));

        Supplier supplier = new Supplier();
        supplier.setTenant(tenant);
        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setCity(request.city());
        supplier.setState(request.state());
        supplier.setPostalCode(request.postalCode());
        supplier.setActive(request.active());
        supplier.setReputationScore(BigDecimal.ZERO);
        return ApiResponse.of(SupplierResponse.from(supplierRepository.save(supplier)));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @Valid @RequestBody UpsertSupplierRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        Supplier supplier = supplierRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "supplier not found"));

        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setCity(request.city());
        supplier.setState(request.state());
        supplier.setPostalCode(request.postalCode());
        supplier.setActive(request.active());
        return ApiResponse.of(SupplierResponse.from(supplierRepository.save(supplier)));
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        Supplier supplier = supplierRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "supplier not found"));
        supplierRepository.delete(supplier);
    }

    public record UpsertSupplierRequest(
            @NotBlank String name,
            String email,
            String phone,
            String address,
            String city,
            String state,
            String postalCode,
            boolean active
    ) {}

    public record SupplierResponse(
            String id,
            String name,
            String email,
            String phone,
            String address,
            String city,
            String state,
            String postalCode,
            boolean active,
            BigDecimal reputationScore
    ) {
        static SupplierResponse from(Supplier supplier) {
            return new SupplierResponse(
                    supplier.getId(),
                    supplier.getName(),
                    supplier.getEmail(),
                    supplier.getPhone(),
                    supplier.getAddress(),
                    supplier.getCity(),
                    supplier.getState(),
                    supplier.getPostalCode(),
                    supplier.isActive(),
                    supplier.getReputationScore()
            );
        }
    }

    public record SupplierProposalResponse(
            String id,
            String orderId,
            String distributionId,
            Proposal.Status status,
            BigDecimal totalPrice,
            Integer deliveryEtaHours,
            java.time.Instant proposedDeliveryAt,
            String message
    ) {
        static SupplierProposalResponse from(Proposal proposal) {
            return new SupplierProposalResponse(
                    proposal.getId(),
                    proposal.getOrderDistribution().getOrder().getId(),
                    proposal.getOrderDistribution().getId(),
                    proposal.getStatus(),
                    proposal.getTotalPrice(),
                    proposal.getDeliveryEtaHours(),
                    proposal.getProposedDeliveryAt(),
                    proposal.getMessage()
            );
        }
    }
}
