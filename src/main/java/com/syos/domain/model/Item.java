package com.syos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Item {
    private String itemCode;
    private String name;
    private String description;
    private String category;
    private BigDecimal unitPrice;
    private int reorderLevel;
    private int reorderQuantity;

    // Constructor for creating a new Item
    public Item(String itemCode, String name, String description, String category, BigDecimal unitPrice, int reorderLevel, int reorderQuantity) {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty.");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative.");
        }
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
        if (reorderQuantity < 0) {
            throw new IllegalArgumentException("Reorder quantity cannot be negative.");
        }

        this.itemCode = itemCode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.unitPrice = unitPrice;
        this.reorderLevel = reorderLevel;
        this.reorderQuantity = reorderQuantity;
    }

    // No-arg constructor (useful for frameworks like JPA)
    public Item() {
    }

    // Getters
    public String getItemCode() {
        return itemCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public int getReorderQuantity() {
        return reorderQuantity;
    }

    // Setters (if the domain model allows these fields to be mutable)
    public void setItemCode(String itemCode) {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        this.itemCode = itemCode;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty.");
        }
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative.");
        }
        this.unitPrice = unitPrice;
    }

    public void setReorderLevel(int reorderLevel) {
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
        this.reorderLevel = reorderLevel;
    }

    public void setReorderQuantity(int reorderQuantity) {
        if (reorderQuantity < 0) {
            throw new IllegalArgumentException("Reorder quantity cannot be negative.");
        }
        this.reorderQuantity = reorderQuantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(itemCode, item.itemCode); // itemCode is the unique identifier
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemCode);
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemCode='" + itemCode + '\'' +
                ", name='" + name + '\'' +
                ", unitPrice=" + unitPrice +
                ", reorderLevel=" + reorderLevel +
                ", reorderQuantity=" + reorderQuantity +
                '}';
    }
}