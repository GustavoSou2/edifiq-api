package com.edifiqapi.domain.supplier;

public class SupplierScoreDTO {
    private Supplier supplier;
    private double score;

    public SupplierScoreDTO(Supplier supplier, double score) {
        this.supplier = supplier;
        this.score = score;
    }

    public Supplier getSupplier() { return supplier; }
    public double getScore() { return score; }
}
