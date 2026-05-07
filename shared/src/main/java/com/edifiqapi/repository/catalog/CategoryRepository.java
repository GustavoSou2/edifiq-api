package com.edifiqapi.repository.catalog;

import com.edifiqapi.domain.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(path = "categories")
public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsBySlug(String slug);

    List<Category> findAllByParentIsNull();

    List<Category> findAllByNameContainingIgnoreCase(String name);

}

