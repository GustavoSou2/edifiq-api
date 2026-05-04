package com.edifiqapi.repository.supplier;

import com.edifiqapi.domain.supplier.SupplierCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "supplier-categories")
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, Long> {
    List<SupplierCategory> findAllBySupplier_Tenant_Id(Long tenantId);

    List<SupplierCategory> findAllBySupplier_IdAndSupplier_Tenant_Id(Long supplierId, Long tenantId);

    Optional<SupplierCategory> findByIdAndSupplier_Tenant_Id(Long id, Long tenantId);
}
