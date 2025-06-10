package com.syos.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public class StockBatch {
    private int batchId;
    private String itemCode;
    private Item item;
    private LocalDate purchaseDate;
    private int quantityReceived;
    private int currentQuantityInStore;
    private LocalDate expiryDate;

    public StockBatch(String itemCode, LocalDate purchaseDate, int quantityReceived, LocalDate expiryDate) {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be null or empty for a stock batch.");
        if (purchaseDate == null) throw new IllegalArgumentException("Purchase date cannot be null.");
        if (quantityReceived <= 0) throw new IllegalArgumentException("Quantity received must be positive.");
        this.itemCode = itemCode;
        this.purchaseDate = purchaseDate;
        this.quantityReceived = quantityReceived;
        this.currentQuantityInStore = quantityReceived;
        this.expiryDate = expiryDate;
    }

    public StockBatch(int batchId, String itemCode, LocalDate purchaseDate, int quantityReceived, int currentQuantityInStore, LocalDate expiryDate) {
        this(itemCode, purchaseDate, quantityReceived, expiryDate);
        this.batchId = batchId;
        this.currentQuantityInStore = currentQuantityInStore;
    }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }
    public String getItemCode() { return itemCode; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public int getQuantityReceived() { return quantityReceived; }
    public int getCurrentQuantityInStore() { return currentQuantityInStore; }
    public void setCurrentQuantityInStore(int currentQuantityInStore) { if (currentQuantityInStore < 0 || currentQuantityInStore > this.quantityReceived) throw new IllegalArgumentException("Current quantity in store cannot be negative or exceed quantity received."); this.currentQuantityInStore = currentQuantityInStore; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockBatch that = (StockBatch) o;
        if (batchId != 0 && that.batchId != 0) return batchId == that.batchId;
        return quantityReceived == that.quantityReceived && currentQuantityInStore == that.currentQuantityInStore && Objects.equals(itemCode, that.itemCode) && Objects.equals(purchaseDate, that.purchaseDate) && Objects.equals(expiryDate, that.expiryDate);
    }

    @Override
    public int hashCode() {
        if (batchId != 0) return Objects.hash(batchId);
        return Objects.hash(itemCode, purchaseDate, quantityReceived, currentQuantityInStore, expiryDate);
    }
}