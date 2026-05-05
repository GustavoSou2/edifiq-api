package com.edifiqapi.repository.order;

import com.edifiqapi.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "orders")
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findAllByTenant_Id(String tenantId);

    Optional<Order> findByIdAndTenant_Id(String id, String tenantId);
}

