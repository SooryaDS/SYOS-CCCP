package com.syos.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class WebsiteStock {
    private Long websiteStockId;
    private Item item;
    private int quantityAvailableOnline;
    private LocalDateTime lastUpdatedDate;

    // No-arg constructor for frameworks (keep this if you need it for JPA/Hibernate)
    public WebsiteStock() {
    }

    /**
     * Full constructor for WebsiteStock, typically used when retrieving from a database.
     * @param websiteStockId The unique ID of this website stock entry.
     * @param item The Item object this website stock is for.
     * @param quantityAvailableOnline The current quantity of the item available online.
     * @param lastUpdatedDate The date and time this entry was last updated.
     */
    public WebsiteStock(Long websiteStockId, Item item, int quantityAvailableOnline, LocalDateTime lastUpdatedDate) {
        if (websiteStockId != null && websiteStockId <= 0) {
            throw new IllegalArgumentException("Website stock ID must be positive if provided.");
        }
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null for website stock.");
        }
        if (quantityAvailableOnline < 0) {
            throw new IllegalArgumentException("Quantity available online cannot be negative.");
        }
        if (lastUpdatedDate == null) {
            throw new IllegalArgumentException("Last updated date cannot be null.");
        }
        this.websiteStockId = websiteStockId;
        this.item = item;
        this.quantityAvailableOnline = quantityAvailableOnline;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    /**
     * Simplified constructor for creating a new WebsiteStock entry, typically before saving to database (ID is null).
     * Automatically sets lastUpdatedDate to now.
     * @param item The Item object this website stock is for.
     * @param quantityAvailableOnline The quantity of the item available online.
     */
    public WebsiteStock(Item item, int quantityAvailableOnline) {
        this(null, item, quantityAvailableOnline, LocalDateTime.now());
    }

    // --- Getters ---
    public Long getWebsiteStockId() { return websiteStockId; }
    public Item getItem() { return item; }
    public int getQuantityAvailableOnline() { return quantityAvailableOnline; }
    public LocalDateTime getLastUpdatedDate() { return lastUpdatedDate; }

    // --- Setters ---
    // REINSTATED: This setter is crucial for persistence layers to set generated IDs.
    public void setWebsiteStockId(Long websiteStockId) {
        this.websiteStockId = websiteStockId;
    }

    // REINSTATED: Add this back if you ever need to change the associated Item (less common for stock records)
    public void setItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null.");
        }
        this.item = item;
    }

    public void setQuantityAvailableOnline(int quantityAvailableOnline) {
        if (quantityAvailableOnline < 0) {
            throw new IllegalArgumentException("Quantity available online cannot be negative.");
        }
        this.quantityAvailableOnline = quantityAvailableOnline;
        this.lastUpdatedDate = LocalDateTime.now(); // Update timestamp on direct quantity set
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public void addQuantity(int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive.");
        }
        this.quantityAvailableOnline += quantityToAdd;
        this.lastUpdatedDate = LocalDateTime.now(); // Update timestamp on modification
    }

    public int reduceQuantity(int quantityToReduce) {
        if (quantityToReduce <= 0) {
            throw new IllegalArgumentException("Quantity to reduce must be positive.");
        }
        // Ensure we don't go below zero
        int actualReduced = Math.min(this.quantityAvailableOnline, quantityToReduce);
        this.quantityAvailableOnline -= actualReduced;
        this.lastUpdatedDate = LocalDateTime.now(); // Update timestamp on modification
        return actualReduced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebsiteStock that = (WebsiteStock) o;
        // Prioritize ID for equality if present, otherwise fall back to Item
        if (websiteStockId != null && that.websiteStockId != null) {
            return Objects.equals(websiteStockId, that.websiteStockId);
        }
        return Objects.equals(item, that.item); // Assuming Item has proper equals/hashCode
    }

    @Override
    public int hashCode() {
        if (websiteStockId != null) {
            return Objects.hash(websiteStockId);
        }
        return Objects.hash(item); // Assuming Item has proper equals/hashCode
    }

    @Override
    public String toString() {
        return "WebsiteStock{" +
                "websiteStockId=" + websiteStockId +
                ", item=" + (item != null ? item.getItemCode() : "null") +
                ", quantityAvailableOnline=" + quantityAvailableOnline +
                ", lastUpdatedDate=" + lastUpdatedDate +
                '}';
    }
}