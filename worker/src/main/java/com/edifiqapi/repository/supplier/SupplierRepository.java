package com.edifiqapi.repository.supplier;

import com.edifiqapi.domain.supplier.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "suppliers")
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByTenant_Id(Long tenantId);

    Optional<Supplier> findByIdAndTenant_Id(Long id, Long tenantId);
}
