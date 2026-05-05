package com.edifiqapi.repository.delivery;

import com.edifiqapi.domain.delivery.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "ratings")
public interface RatingRepository extends JpaRepository<Rating, String> {
    List<Rating> findAllByOrderSelection_Order_Tenant_Id(String tenantId);

    Optional<Rating> findByIdAndOrderSelection_Order_Tenant_Id(String id, String tenantId);
}

