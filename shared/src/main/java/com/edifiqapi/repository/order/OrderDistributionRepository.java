package com.edifiqapi.repository.order;

import com.edifiqapi.domain.order.OrderDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "order-distributions")
public interface OrderDistributionRepository extends JpaRepository<OrderDistribution, String> {
    List<OrderDistribution> findAllByOrder_IdAndBuyerTenant_Id(String orderId, String tenantId);

    Optional<OrderDistribution> findByIdAndBuyerTenant_Id(String id, String tenantId);

    Optional<OrderDistribution> findByIdAndSupplierTenant_Id(String id, String supplierTenantId);

    List<OrderDistribution> findAllBySupplierTenant_Id(String supplierTenantId);
}

