package com.edifiqapi.repository.order;

import com.edifiqapi.domain.order.OrderSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "order-selections")
public interface OrderSelectionRepository extends JpaRepository<OrderSelection, String> {
    Optional<OrderSelection> findByOrder_IdAndOrder_Tenant_Id(String orderId, String tenantId);

    Optional<OrderSelection> findByIdAndOrder_Tenant_Id(String id, String tenantId);
}

