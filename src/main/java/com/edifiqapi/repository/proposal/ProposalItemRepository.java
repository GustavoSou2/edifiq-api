package com.edifiqapi.repository.proposal;

import com.edifiqapi.domain.proposal.ProposalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "proposal-items")
public interface ProposalItemRepository extends JpaRepository<ProposalItem, Long> {
    List<ProposalItem> findAllByProposal_Id(Long proposalId);

    Optional<ProposalItem> findByIdAndProposal_OrderDistribution_Order_Tenant_Id(Long id, Long tenantId);
}
