package com.edifiqapi.repository.proposal;

import com.edifiqapi.domain.proposal.ProposalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "proposal-items")
public interface ProposalItemRepository extends JpaRepository<ProposalItem, String> {
    List<ProposalItem> findAllByProposal_Id(String proposalId);

    Optional<ProposalItem> findByIdAndProposal_OrderDistribution_Order_Tenant_Id(String id, String tenantId);
}

