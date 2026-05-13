package com.edifiqapi.repository.proposal;

import com.edifiqapi.domain.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "proposals")
public interface ProposalRepository extends JpaRepository<Proposal, String> {
    List<Proposal> findAllByOrderDistribution_Order_IdAndOrderDistribution_Order_Tenant_Id(String orderId, String tenantId);
    List<Proposal> findAllByOrderDistribution_Supplier_IdAndOrderDistribution_SupplierTenant_Id(String supplierId, String supplierTenantId);

    Optional<Proposal> findByIdAndOrderDistribution_Order_Tenant_Id(String id, String tenantId);
}

