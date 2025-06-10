package com.syos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Item {
    private String itemCode;
    private String name;
    private String description;
    private String category;
    private BigDecimal unitPrice;
    private int reorderLevelThreshold;
    private int minShelfStockThreshold;

    public Item(String itemCode, String name, String description, String category, BigDecimal unitPrice, int reorderLevelThreshold, int minShelfStockThreshold) {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be null or empty.");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Item name cannot be null or empty.");
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Unit price cannot be null or negative.");
        if (reorderLevelThreshold < 0) throw new IllegalArgumentException("Reorder level threshold cannot be negative.");
        if (minShelfStockThreshold < 0) throw new IllegalArgumentException("Minimum shelf stock threshold cannot be negative.");
        this.itemCode = itemCode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.unitPrice = unitPrice;
        this.reorderLevelThreshold = reorderLevelThreshold;
        this.minShelfStockThreshold = minShelfStockThreshold;
    }

    public String getItemCode() { return itemCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getReorderLevelThreshold() { return reorderLevelThreshold; }
    public int getMinShelfStockThreshold() { return minShelfStockThreshold; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setReorderLevelThreshold(int reorderLevelThreshold) { this.reorderLevelThreshold = reorderLevelThreshold; }
    public void setMinShelfStockThreshold(int minShelfStockThreshold) { this.minShelfStockThreshold = minShelfStockThreshold; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(itemCode, item.itemCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemCode);
    }

    @Override
    public String toString() {
        return "Item{" + "itemCode='" + itemCode + '\'' + ", name='" + name + '\'' + ", unitPrice=" + String.format("%.2f", unitPrice) + ", reorderLevelThreshold=" + reorderLevelThreshold + ", minShelfStockThreshold=" + minShelfStockThreshold + '}';
    }
}