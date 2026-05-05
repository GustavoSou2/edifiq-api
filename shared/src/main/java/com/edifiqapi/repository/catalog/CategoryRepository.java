package com.edifiqapi.repository.catalog;

import com.edifiqapi.domain.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "categories")
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByTenant_Id(Long tenantId);

    Optional<Category> findByIdAndTenant_Id(Long id, Long tenantId);
}
