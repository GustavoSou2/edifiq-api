package com.edifiqapi.repository.proposal;

import com.edifiqapi.domain.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "proposals")
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findAllByOrderDistribution_Order_IdAndOrderDistribution_Order_Tenant_Id(Long orderId, Long tenantId);

    Optional<Proposal> findByIdAndOrderDistribution_Order_Tenant_Id(Long id, Long tenantId);
}
