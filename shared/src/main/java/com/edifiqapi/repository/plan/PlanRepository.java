package com.edifiqapi.repository.plan;

import com.edifiqapi.domain.plan.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path = "plans")
public interface PlanRepository extends JpaRepository<Plan, String> {
    Optional<Plan> findByName(String name);
}

