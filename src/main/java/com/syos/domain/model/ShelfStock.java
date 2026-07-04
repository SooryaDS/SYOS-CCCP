package com.syos.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class ShelfStock {
    private Long shelfStockId; // Changed from int to Long for consistency and best practice
    private Item item; // Changed from String itemCode to Item object
    // private int batchId; // Removed - assuming ShelfStock is an aggregate view per item, not per batch
    private int quantityOnShelf;
    private LocalDateTime lastUpdatedDate; // Renamed for general purpose clarity

    // No-arg constructor for frameworks (e.g., ORMs, JSON deserialization)
    public ShelfStock() {
    }

    /**
     * Full constructor for ShelfStock, typically used when retrieving from a database.
     * @param shelfStockId The unique ID of this shelf stock entry.
     * @param item The Item object this shelf stock is for.
     * @param quantityOnShelf The current quantity of the item on the shelf.
     * @param lastUpdatedDate The date and time this entry was last updated.
     */
    public ShelfStock(Long shelfStockId, Item item, int quantityOnShelf, LocalDateTime lastUpdatedDate) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null for shelf stock.");
        if (quantityOnShelf < 0) throw new IllegalArgumentException("Quantity on shelf cannot be negative.");
        this.shelfStockId = shelfStockId;
        this.item = item;
        this.quantityOnShelf = quantityOnShelf;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    /**
     * Constructor for creating a new ShelfStock entry, typically before saving to database (ID is null).
     * Automatically sets lastUpdatedDate to now.
     * @param item The Item object this shelf stock is for.
     * @param quantityOnShelf The current quantity of the item on the shelf.
     */
    public ShelfStock(Item item, int quantityOnShelf) {
        this(null, item, quantityOnShelf, LocalDateTime.now());
    }

    // --- Getters ---
    public Long getShelfStockId() { return shelfStockId; }
    public Item getItem() { return item; }
    public int getQuantityOnShelf() { return quantityOnShelf; }
    public LocalDateTime getLastUpdatedDate() { return lastUpdatedDate; } // Renamed getter

    // --- Setters ---
    public void setShelfStockId(Long shelfStockId) { this.shelfStockId = shelfStockId; }
    public void setItem(Item item) { this.item = item; }
    public void setQuantityOnShelf(int quantityOnShelf) {
        if (quantityOnShelf < 0) throw new IllegalArgumentException("Quantity on shelf cannot be negative.");
        this.quantityOnShelf = quantityOnShelf;
    }
    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) { this.lastUpdatedDate = lastUpdatedDate; } // Renamed setter

    // Generic getQuantity/setQuantity for compatibility with common interface patterns
    public int getQuantity() { return getQuantityOnShelf(); }
    public void setQuantity(int quantity) { setQuantityOnShelf(quantity); }

    public void addQuantity(int quantityToAdd) {
        if (quantityToAdd <= 0) throw new IllegalArgumentException("Quantity to add must be positive.");
        this.quantityOnShelf += quantityToAdd;
        this.lastUpdatedDate = LocalDateTime.now(); // Update timestamp on modification
    }

    public int reduceQuantity(int quantityToReduce) {
        if (quantityToReduce <= 0) throw new IllegalArgumentException("Quantity to reduce must be positive.");
        int actualReduced = Math.min(this.quantityOnShelf, quantityToReduce);
        this.quantityOnShelf -= actualReduced;
        this.lastUpdatedDate = LocalDateTime.now(); // Update timestamp on modification
        return actualReduced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShelfStock that = (ShelfStock) o;
        // For an aggregated ShelfStock, equality is typically based on the Item it represents.
        // If IDs are present, they are the primary key.
        if (shelfStockId != null && that.shelfStockId != null) {
            return Objects.equals(shelfStockId, that.shelfStockId);
        }
        return Objects.equals(item, that.item); // Assuming Item has proper equals/hashCode
    }

    @Override
    public int hashCode() {
        if (shelfStockId != null) {
            return Objects.hash(shelfStockId);
        }
        return Objects.hash(item); // Assuming Item has proper equals/hashCode
    }

    @Override
    public String toString() {
        return "ShelfStock{" +
                "shelfStockId=" + shelfStockId +
                ", item=" + (item != null ? item.getItemCode() : "null") + // Display item code for clarity
                ", quantityOnShelf=" + quantityOnShelf +
                ", lastUpdatedDate=" + lastUpdatedDate +
                '}';
    }
}