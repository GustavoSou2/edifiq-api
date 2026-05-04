package com.edifiqapi.repository.order;

import com.edifiqapi.domain.order.OrderSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "order-selections")
public interface OrderSelectionRepository extends JpaRepository<OrderSelection, Long> {
    Optional<OrderSelection> findByOrder_IdAndOrder_Tenant_Id(Long orderId, Long tenantId);

    Optional<OrderSelection> findByIdAndOrder_Tenant_Id(Long id, Long tenantId);
}
