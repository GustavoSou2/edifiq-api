package com.edifiqapi.repository.order;

import com.edifiqapi.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "order-items")
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findAllByOrder_Id(Long orderId);

    Optional<OrderItem> findByIdAndOrder_Tenant_Id(Long id, Long tenantId);
}
