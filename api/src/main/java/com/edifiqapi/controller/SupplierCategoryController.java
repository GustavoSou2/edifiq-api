package com.edifiqapi.controller;

import com.edifiqapi.domain.supplier.SupplierCategory;
import com.edifiqapi.repository.catalog.CategoryRepository;
import com.edifiqapi.repository.supplier.SupplierCategoryRepository;
import com.edifiqapi.repository.supplier.SupplierRepository;
import com.edifiqapi.security.JwtClaims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/supplier-categories")
public class SupplierCategoryController {
    private final SupplierCategoryRepository supplierCategoryRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;

    public SupplierCategoryController(
            SupplierCategoryRepository supplierCategoryRepository,
            SupplierRepository supplierRepository,
            CategoryRepository categoryRepository
    ) {
        this.supplierCategoryRepository = supplierCategoryRepository;
        this.supplierRepository = supplierRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<SupplierCategoryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return supplierCategoryRepository.findAllBySupplier_Tenant_Id(tenantId).stream()
                .map(SupplierCategoryResponse::from)
                .toList();
    }

    @PostMapping
    public SupplierCategoryResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateSupplierCategoryRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        var supplier = supplierRepository.findByIdAndTenant_Id(request.supplierId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "supplier not found"));
        var category = categoryRepository.findByIdAndTenant_Id(request.categoryId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "category not found"));

        SupplierCategory sc = new SupplierCategory();
        sc.setSupplier(supplier);
        sc.setCategory(category);
        return SupplierCategoryResponse.from(supplierCategoryRepository.save(sc));
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        SupplierCategory sc = supplierCategoryRepository.findByIdAndSupplier_Tenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "supplier category not found"));
        supplierCategoryRepository.delete(sc);
    }

    public record CreateSupplierCategoryRequest(@NotNull String supplierId, @NotNull String categoryId) {}

    public record SupplierCategoryResponse(String id, String supplierId, String categoryId) {
        static SupplierCategoryResponse from(SupplierCategory sc) {
            return new SupplierCategoryResponse(sc.getId(), sc.getSupplier().getId(), sc.getCategory().getId());
        }
    }
}



