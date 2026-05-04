package com.edifiqapi.domain.order;

import com.edifiqapi.domain.BaseEntity;
import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.rbac.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "order_selections")
public class OrderSelection extends BaseEntity {
    public enum Status {
        selected,
        cancelled
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_by", nullable = false)
    private User selectedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    @jakarta.persistence.PrePersist
    void prePersistOrderSelection() {
        if (selectedAt == null) {
            selectedAt = Instant.now();
        }
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public User getSelectedBy() {
        return selectedBy;
    }

    public void setSelectedBy(User selectedBy) {
        this.selectedBy = selectedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(Instant selectedAt) {
        this.selectedAt = selectedAt;
    }
}
