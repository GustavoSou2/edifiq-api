package com.edifiqapi.controller;

import com.edifiqapi.domain.catalog.Category;
import com.edifiqapi.repository.catalog.CategoryRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import com.edifiqapi.security.JwtClaims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;

    public CategoryController(CategoryRepository categoryRepository, TenantRepository tenantRepository) {
        this.categoryRepository = categoryRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        long tenantId = JwtClaims.tenantId(jwt);
        return categoryRepository.findAllByTenant_Id(tenantId).stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        long tenantId = JwtClaims.tenantId(jwt);
        return categoryRepository.findByIdAndTenant_Id(id, tenantId).map(CategoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "category not found"));
    }

    @PostMapping
    public CategoryResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertCategoryRequest request) {
        long tenantId = JwtClaims.tenantId(jwt);
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));

        Category category = new Category();
        category.setTenant(tenant);
        category.setName(request.name());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody UpsertCategoryRequest request) {
        long tenantId = JwtClaims.tenantId(jwt);
        Category category = categoryRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "category not found"));
        category.setName(request.name());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        long tenantId = JwtClaims.tenantId(jwt);
        Category category = categoryRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "category not found"));
        categoryRepository.delete(category);
    }

    public record UpsertCategoryRequest(@NotBlank String name) {}

    public record CategoryResponse(Long id, String name) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName());
        }
    }
}

