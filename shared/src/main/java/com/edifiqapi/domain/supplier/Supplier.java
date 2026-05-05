package com.edifiqapi.domain.supplier;

import com.edifiqapi.domain.BaseEntity;
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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {
    public enum Status {
        active,
        inactive,
        blocked
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 18)
    private String cnpj;

    @Column(nullable = false)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(columnDefinition = "text")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(name = "zip_code", length = 9)
    private String zipCode;

    @Column
    private BigDecimal lat;

    @Column
    private BigDecimal lng;

    @Column(name = "reputation_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal reputationScore;

    @Column(name = "total_ratings", nullable = false)
    private int totalRatings;

    @Column(name = "total_deliveries", nullable = false)
    private int totalDeliveries;

    @Column(name = "max_delivery_km", nullable = false)
    private int maxDeliveryKm;

    @Column(name = "response_sla_min", nullable = false)
    private int responseSlaMin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersistSupplier() {
        var now = Instant.now();
        if (status == null) {
            status = Status.active;
        }
        if (companyName == null || companyName.isBlank()) {
            companyName = email;
        }
        if (cnpj == null || cnpj.isBlank()) {
            cnpj = getId() != null ? getId().substring(0, Math.min(14, getId().length())) : "00000000000000";
        }
        if (reputationScore == null) {
            reputationScore = BigDecimal.ZERO;
        }
        if (maxDeliveryKm == 0) {
            maxDeliveryKm = 50;
        }
        if (responseSlaMin == 0) {
            responseSlaMin = 60;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getName() {
        return companyName;
    }

    public void setName(String name) {
        this.companyName = name;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == Status.active;
    }

    public void setActive(boolean active) {
        this.status = active ? Status.active : Status.inactive;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return zipCode;
    }

    public void setPostalCode(String postalCode) {
        this.zipCode = postalCode;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }

    public BigDecimal getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(BigDecimal reputationScore) {
        this.reputationScore = reputationScore;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public int getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(int totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public int getMaxDeliveryKm() {
        return maxDeliveryKm;
    }

    public void setMaxDeliveryKm(int maxDeliveryKm) {
        this.maxDeliveryKm = maxDeliveryKm;
    }

    public int getResponseSlaMin() {
        return responseSlaMin;
    }

    public void setResponseSlaMin(int responseSlaMin) {
        this.responseSlaMin = responseSlaMin;
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
}
