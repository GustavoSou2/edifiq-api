package com.edifiqapi.controller;

import com.edifiqapi.domain.catalog.Category;
import com.edifiqapi.repository.catalog.CategoryRepository;
import com.edifiqapi.service.AuthService;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** Returns only root categories, each with their full subtree nested. */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.of(categoryRepository.findAllByParentIsNull().stream()
                .map(CategoryResponse::from)
                .toList());
    }

    /** Flat search across all categories (no nesting) filtered by name. */
    @GetMapping("/search")
    public ApiResponse<List<CategoryFlatResponse>> search(@RequestParam String q) {
        return ApiResponse.of(categoryRepository.findAllByNameContainingIgnoreCase(q).stream()
                .map(CategoryFlatResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> get(@PathVariable String id) {
        return ApiResponse.of(categoryRepository.findById(id).map(CategoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "category not found")));
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody UpsertCategoryRequest request) {
        String slug = AuthService.toSlug(request.name());

        if (categoryRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(CONFLICT, "category slug already exists: " + slug);
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(slug);

        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "parent category not found"));
            category.setParent(parent);
        }

        return ApiResponse.of(CategoryResponse.from(categoryRepository.save(category)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable String id, @Valid @RequestBody UpsertCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "category not found"));

        String slug = AuthService.toSlug(request.name());

        if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(CONFLICT, "category slug already exists: " + slug);
        }

        category.setName(request.name());
        category.setSlug(slug);

        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return ApiResponse.of(CategoryResponse.from(categoryRepository.save(category)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "category not found"));
        categoryRepository.delete(category);
    }

    public record UpsertCategoryRequest(@NotBlank String name, String parentId) {}

    public record CategoryFlatResponse(String id, String name, String slug, String parentId) {
        static CategoryFlatResponse from(Category category) {
            return new CategoryFlatResponse(
                    category.getId(),
                    category.getName(),
                    category.getSlug(),
                    category.getParent() != null ? category.getParent().getId() : null
            );
        }
    }

    public record CategoryResponse(String id, String name, String slug, String parentId, List<CategoryResponse> children) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getSlug(),
                    category.getParent() != null ? category.getParent().getId() : null,
                    category.getChildren().stream().map(CategoryResponse::from).toList()
            );
        }
    }
}
