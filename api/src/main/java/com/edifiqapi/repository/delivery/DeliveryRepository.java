package com.edifiqapi.repository.delivery;

import com.edifiqapi.domain.delivery.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "deliveries")
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByIdAndOrderSelection_Order_Tenant_Id(Long id, Long tenantId);

    Optional<Delivery> findByOrderSelection_IdAndOrderSelection_Order_Tenant_Id(Long selectionId, Long tenantId);
}
