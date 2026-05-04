package com.edifiqapi.domain.plan;

import com.edifiqapi.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
public class Plan extends BaseEntity {
    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "max_users", nullable = false)
    private int maxUsers;

    @Column(name = "max_suppliers", nullable = false)
    private int maxSuppliers;

    @Column(name = "max_orders_per_month", nullable = false)
    private int maxOrdersPerMonth;

    @Column(name = "has_analytics", nullable = false)
    private boolean hasAnalytics;

    @Column(name = "has_api_access", nullable = false)
    private boolean hasApiAccess;

    @Column(name = "price_monthly", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMonthly;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }

    public int getMaxSuppliers() {
        return maxSuppliers;
    }

    public void setMaxSuppliers(int maxSuppliers) {
        this.maxSuppliers = maxSuppliers;
    }

    public int getMaxOrdersPerMonth() {
        return maxOrdersPerMonth;
    }

    public void setMaxOrdersPerMonth(int maxOrdersPerMonth) {
        this.maxOrdersPerMonth = maxOrdersPerMonth;
    }

    public boolean isHasAnalytics() {
        return hasAnalytics;
    }

    public void setHasAnalytics(boolean hasAnalytics) {
        this.hasAnalytics = hasAnalytics;
    }

    public boolean isHasApiAccess() {
        return hasApiAccess;
    }

    public void setHasApiAccess(boolean hasApiAccess) {
        this.hasApiAccess = hasApiAccess;
    }

    public BigDecimal getPriceMonthly() {
        return priceMonthly;
    }

    public void setPriceMonthly(BigDecimal priceMonthly) {
        this.priceMonthly = priceMonthly;
    }
}

