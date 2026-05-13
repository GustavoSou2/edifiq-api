package com.edifiqapi.repository.order;

import com.edifiqapi.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "order-items")
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findAllByOrder_Id(String orderId);

    Optional<OrderItem> findByIdAndOrder_Tenant_Id(String id, String tenantId);

    Optional<OrderItem> findByIdAndOrder_Id(String id, String orderId);
}

