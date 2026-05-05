package com.edifiqapi.repository.delivery;

import com.edifiqapi.domain.delivery.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "deliveries")
public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    Optional<Delivery> findByIdAndOrderSelection_Order_Tenant_Id(String id, String tenantId);

    Optional<Delivery> findByOrderSelection_IdAndOrderSelection_Order_Tenant_Id(String selectionId, String tenantId);
}

