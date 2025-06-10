package com.syos.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class ShelfStock {
    private int shelfStockId;
    private String itemCode;
    private int batchId;
    private int quantityOnShelf;
    private LocalDateTime lastStockedDate;

    public ShelfStock(String itemCode, int batchId, int quantityOnShelf) {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be null or empty for shelf stock.");
        if (batchId <= 0) throw new IllegalArgumentException("Batch ID must be positive for shelf stock.");
        if (quantityOnShelf < 0) throw new IllegalArgumentException("Quantity on shelf cannot be negative.");
        this.itemCode = itemCode;
        this.batchId = batchId;
        this.quantityOnShelf = quantityOnShelf;
        this.lastStockedDate = LocalDateTime.now();
    }

    public ShelfStock(int shelfStockId, String itemCode, int batchId, int quantityOnShelf, LocalDateTime lastStockedDate) {
        this(itemCode, batchId, quantityOnShelf);
        this.shelfStockId = shelfStockId;
        this.lastStockedDate = lastStockedDate;
    }

    public int getShelfStockId() { return shelfStockId; }
    public void setShelfStockId(int shelfStockId) { this.shelfStockId = shelfStockId; }
    public String getItemCode() { return itemCode; }
    public int getBatchId() { return batchId; }
    public int getQuantityOnShelf() { return quantityOnShelf; }
    public void setQuantityOnShelf(int quantityOnShelf) { if (quantityOnShelf < 0) throw new IllegalArgumentException("Quantity on shelf cannot be negative."); this.quantityOnShelf = quantityOnShelf; }
    public LocalDateTime getLastStockedDate() { return lastStockedDate; }
    public void setLastStockedDate(LocalDateTime lastStockedDate) { this.lastStockedDate = lastStockedDate; }

    public void addQuantity(int quantityToAdd) {
        if (quantityToAdd <= 0) throw new IllegalArgumentException("Quantity to add must be positive.");
        this.quantityOnShelf += quantityToAdd;
        this.lastStockedDate = LocalDateTime.now();
    }

    public int reduceQuantity(int quantityToReduce) {
        if (quantityToReduce <= 0) throw new IllegalArgumentException("Quantity to reduce must be positive.");
        int actualReduced = Math.min(this.quantityOnShelf, quantityToReduce);
        this.quantityOnShelf -= actualReduced;
        return actualReduced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShelfStock that = (ShelfStock) o;
        if (shelfStockId != 0 && that.shelfStockId != 0) return shelfStockId == that.shelfStockId;
        return batchId == that.batchId && Objects.equals(itemCode, that.itemCode);
    }

    @Override
    public int hashCode() {
        if (shelfStockId != 0) return Objects.hash(shelfStockId);
        return Objects.hash(itemCode, batchId);
    }
}