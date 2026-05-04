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
        queued,
        processing,
        sent,
        failed,
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

    @Column(name = "queue_message_id", length = 100)
    private String queueMessageId;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "processing_at")
    private Instant processingAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "dispatch_attempts", nullable = false)
    private int dispatchAttempts;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

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

    public String getQueueMessageId() {
        return queueMessageId;
    }

    public void setQueueMessageId(String queueMessageId) {
        this.queueMessageId = queueMessageId;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Instant getProcessingAt() {
        return processingAt;
    }

    public void setProcessingAt(Instant processingAt) {
        this.processingAt = processingAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public int getDispatchAttempts() {
        return dispatchAttempts;
    }

    public void setDispatchAttempts(int dispatchAttempts) {
        this.dispatchAttempts = dispatchAttempts;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
