package com.edifiqapi.domain.order;

import com.edifiqapi.domain.BaseEntity;
import com.edifiqapi.domain.supplier.Supplier;
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
@Table(name = "order_distributions")
public class OrderDistribution extends BaseEntity {
    public enum Status {
        pending,
        delivered,
        expired,
        declined
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "distributed_at", nullable = false)
    private Instant distributedAt;

    @jakarta.persistence.PrePersist
    void prePersistOrderDistribution() {
        if (distributedAt == null) {
            distributedAt = Instant.now();
        }
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getDistributedAt() {
        return distributedAt;
    }

    public void setDistributedAt(Instant distributedAt) {
        this.distributedAt = distributedAt;
    }
}
