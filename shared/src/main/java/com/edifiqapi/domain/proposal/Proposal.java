package com.edifiqapi.domain.proposal;

import com.edifiqapi.domain.BaseEntity;
import com.edifiqapi.domain.order.OrderDistribution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "proposals")
public class Proposal extends BaseEntity {
    public enum Status {
        submitted,
        updated,
        withdrawn
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_distribution_id", nullable = false)
    private OrderDistribution orderDistribution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "delivery_eta_hours")
    private Integer deliveryEtaHours;

    @Column(name = "proposed_delivery_at")
    private Instant proposedDeliveryAt;

    @Column(columnDefinition = "text")
    private String message;

    public OrderDistribution getOrderDistribution() {
        return orderDistribution;
    }

    public void setOrderDistribution(OrderDistribution orderDistribution) {
        this.orderDistribution = orderDistribution;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Integer getDeliveryEtaHours() {
        return deliveryEtaHours;
    }

    public void setDeliveryEtaHours(Integer deliveryEtaHours) {
        this.deliveryEtaHours = deliveryEtaHours;
    }

    public Instant getProposedDeliveryAt() {
        return proposedDeliveryAt;
    }

    public void setProposedDeliveryAt(Instant proposedDeliveryAt) {
        this.proposedDeliveryAt = proposedDeliveryAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

