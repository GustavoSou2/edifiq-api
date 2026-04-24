package com.edifiqapi.controller;

import com.edifiqapi.repository.plan.PlanRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/plans")
public class PlanController {
    private final PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public List<PlanResponse> list() {
        return planRepository.findAll().stream().map(PlanResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PlanResponse get(@PathVariable Long id) {
        return planRepository.findById(id).map(PlanResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "plan not found"));
    }

    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody UpsertPlanRequest request) {
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "plan not found"));
        plan.setName(request.name());
        plan.setMaxUsers(request.maxUsers());
        plan.setMaxSuppliers(request.maxSuppliers());
        plan.setMaxOrdersPerMonth(request.maxOrdersPerMonth());
        plan.setHasAnalytics(request.hasAnalytics());
        plan.setHasApiAccess(request.hasApiAccess());
        plan.setPriceMonthly(request.priceMonthly());
        return PlanResponse.from(planRepository.save(plan));
    }

    public record UpsertPlanRequest(
            @NotBlank String name,
            int maxUsers,
            int maxSuppliers,
            int maxOrdersPerMonth,
            boolean hasAnalytics,
            boolean hasApiAccess,
            BigDecimal priceMonthly
    ) {}

    public record PlanResponse(
            Long id,
            String name,
            int maxUsers,
            int maxSuppliers,
            int maxOrdersPerMonth,
            boolean hasAnalytics,
            boolean hasApiAccess,
            BigDecimal priceMonthly
    ) {
        static PlanResponse from(com.edifiqapi.domain.plan.Plan plan) {
            return new PlanResponse(
                    plan.getId(),
                    plan.getName(),
                    plan.getMaxUsers(),
                    plan.getMaxSuppliers(),
                    plan.getMaxOrdersPerMonth(),
                    plan.isHasAnalytics(),
                    plan.isHasApiAccess(),
                    plan.getPriceMonthly()
            );
        }
    }
}

