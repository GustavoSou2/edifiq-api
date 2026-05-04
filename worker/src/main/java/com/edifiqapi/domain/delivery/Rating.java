package com.edifiqapi.domain.delivery;

import com.edifiqapi.domain.BaseEntity;
import com.edifiqapi.domain.order.OrderSelection;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.supplier.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ratings")
public class Rating extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_selection_id", nullable = false)
    private OrderSelection orderSelection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rated_by", nullable = false)
    private User ratedBy;

    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(columnDefinition = "text")
    private String response;

    public OrderSelection getOrderSelection() {
        return orderSelection;
    }

    public void setOrderSelection(OrderSelection orderSelection) {
        this.orderSelection = orderSelection;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public User getRatedBy() {
        return ratedBy;
    }

    public void setRatedBy(User ratedBy) {
        this.ratedBy = ratedBy;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

