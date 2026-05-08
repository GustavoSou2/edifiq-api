package com.edifiqapi.domain.order;

import com.edifiqapi.domain.BaseEntity;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    public enum Status {
        draft,
        open,
        in_auction,
        selected,
        confirmed,
        cancelled,
        expired
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "is_urgent", nullable = false)
    private boolean urgent;

    @Column(name = "delivery_address", columnDefinition = "text", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_state", length = 2)
    private String deliveryState;

    @Column(name = "delivery_lat")
    private Double deliveryLat;

    @Column(name = "delivery_lng")
    private Double deliveryLng;

    @Column(name = "delivery_window_start")
    private Instant deliveryWindowStart;

    @Column(name = "delivery_window_end")
    private Instant deliveryWindowEnd;

    @Column(name = "max_suppliers", nullable = false)
    private int maxSuppliers;

    @Column(name = "auction_duration_min", nullable = false)
    private int auctionDurationMin;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "reference_code", length = 50)
    private String referenceCode;

    @Column(name = "title", length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> metadata;

    @PrePersist
    void prePersistOrder() {
        var now = Instant.now();
        if (status == null) {
            status = Status.draft;
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            deliveryAddress = "A definir";
        }
        if (maxSuppliers == 0) {
            maxSuppliers = 10;
        }
        if (auctionDurationMin == 0) {
            auctionDurationMin = 60;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        createdAt = now;
        updatedAt = now;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public String getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(String deliveryState) {
        this.deliveryState = deliveryState;
    }

    public Double getDeliveryLat() {
        return deliveryLat;
    }

    public void setDeliveryLat(Double deliveryLat) {
        this.deliveryLat = deliveryLat;
    }

    public Double getDeliveryLng() {
        return deliveryLng;
    }

    public void setDeliveryLng(Double deliveryLng) {
        this.deliveryLng = deliveryLng;
    }

    public Instant getDeliveryWindowStart() {
        return deliveryWindowStart;
    }

    public void setDeliveryWindowStart(Instant deliveryWindowStart) {
        this.deliveryWindowStart = deliveryWindowStart;
    }

    public Instant getDeliveryWindowEnd() {
        return deliveryWindowEnd;
    }

    public void setDeliveryWindowEnd(Instant deliveryWindowEnd) {
        this.deliveryWindowEnd = deliveryWindowEnd;
    }

    public int getMaxSuppliers() {
        return maxSuppliers;
    }

    public void setMaxSuppliers(int maxSuppliers) {
        this.maxSuppliers = maxSuppliers;
    }

    public int getAuctionDurationMin() {
        return auctionDurationMin;
    }

    public void setAuctionDurationMin(int auctionDurationMin) {
        this.auctionDurationMin = auctionDurationMin;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
