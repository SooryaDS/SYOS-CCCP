package com.syos.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockBatch {
    private Long batchId;
    private Item item;
    private int receivedQuantity;
    private int currentQuantityInStore;
    private LocalDate expiryDate;
    private BigDecimal costPerUnit;
    private LocalDate receivedDate;

    // No-arg constructor for frameworks that require it (e.g., ORMs, JSON deserialization)
    public StockBatch() {
    }

    // Full constructor used in tests and potentially in application logic
    public StockBatch(Long batchId, Item item, int receivedQuantity, int currentQuantityInStore, LocalDate expiryDate, BigDecimal costPerUnit, LocalDate receivedDate) {
        this.batchId = batchId;
        this.item = item;
        this.receivedQuantity = receivedQuantity;
        this.currentQuantityInStore = currentQuantityInStore;
        this.expiryDate = expiryDate;
        this.costPerUnit = costPerUnit;
        this.receivedDate = receivedDate;
    }

    // --- Getters ---
    public Long getBatchId() {
        return batchId;
    }

    public Item getItem() {
        return item;
    }

    public int getReceivedQuantity() {
        return receivedQuantity;
    }

    public int getCurrentQuantityInStore() {
        return currentQuantityInStore;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    // --- Setters ---
    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setReceivedQuantity(int receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public void setCurrentQuantityInStore(int currentQuantityInStore) {
        this.currentQuantityInStore = currentQuantityInStore;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    @Override
    public String toString() {
        return "StockBatch{" +
                "batchId=" + batchId +
                ", item=" + (item != null ? item.getItemCode() : "null") +
                ", receivedQuantity=" + receivedQuantity +
                ", currentQuantityInStore=" + currentQuantityInStore +
                ", expiryDate=" + expiryDate +
                ", costPerUnit=" + costPerUnit +
                ", receivedDate=" + receivedDate +
                '}';
    }
}