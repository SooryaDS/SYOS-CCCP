package com.syos.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class WebsiteStock {
    private int websiteStockId;
    private String itemCode;
    private int batchId;
    private int quantityAvailableOnline;
    private LocalDateTime lastUpdatedDate;

    public WebsiteStock(String itemCode, int batchId, int quantityAvailableOnline) {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be null or empty for website stock.");
        if (batchId <= 0) throw new IllegalArgumentException("Batch ID must be positive for website stock.");
        if (quantityAvailableOnline < 0) throw new IllegalArgumentException("Quantity available online cannot be negative.");
        this.itemCode = itemCode;
        this.batchId = batchId;
        this.quantityAvailableOnline = quantityAvailableOnline;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public WebsiteStock(int websiteStockId, String itemCode, int batchId, int quantityAvailableOnline, LocalDateTime lastUpdatedDate) {
        this(itemCode, batchId, quantityAvailableOnline);
        this.websiteStockId = websiteStockId;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public int getWebsiteStockId() { return websiteStockId; }
    public void setWebsiteStockId(int websiteStockId) { this.websiteStockId = websiteStockId; }
    public String getItemCode() { return itemCode; }
    public int getBatchId() { return batchId; }
    public int getQuantityAvailableOnline() { return quantityAvailableOnline; }
    public void setQuantityAvailableOnline(int quantityAvailableOnline) { if (quantityAvailableOnline < 0) throw new IllegalArgumentException("Quantity available online cannot be negative."); this.quantityAvailableOnline = quantityAvailableOnline; }
    public LocalDateTime getLastUpdatedDate() { return lastUpdatedDate; }
    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) { this.lastUpdatedDate = lastUpdatedDate; }

    public void addQuantity(int quantityToAdd) {
        if (quantityToAdd <= 0) throw new IllegalArgumentException("Quantity to add must be positive.");
        this.quantityAvailableOnline += quantityToAdd;
        this.lastUpdatedDate = LocalDateTime.now();
    }

    public int reduceQuantity(int quantityToReduce) {
        if (quantityToReduce <= 0) throw new IllegalArgumentException("Quantity to reduce must be positive.");
        int actualReduced = Math.min(this.quantityAvailableOnline, quantityToReduce);
        this.quantityAvailableOnline -= actualReduced;
        return actualReduced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebsiteStock that = (WebsiteStock) o;
        if (websiteStockId != 0 && that.websiteStockId != 0) return websiteStockId == that.websiteStockId;
        return batchId == that.batchId && Objects.equals(itemCode, that.itemCode);
    }

    @Override
    public int hashCode() {
        if (websiteStockId != 0) return Objects.hash(websiteStockId);
        return Objects.hash(itemCode, batchId);
    }
}