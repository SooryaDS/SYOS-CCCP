package com.syos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class BillItem {
    private int id; // Auto-generated ID for database persistence
    private int billId; // Foreign key to the parent Bill (will be set by BillRepository)
    private String billSerialNumber; // Redundant but useful for reports/debugging
    private Item item; // Reference to the actual Item object
    private String itemNameAtSale; // Snapshot of item name at time of sale
    private int quantityPurchased;
    private BigDecimal pricePerUnitAtSale; // Snapshot of unit price at time of sale
    private BigDecimal totalPriceForItem; // Calculated total for this line item (quantity * pricePerUnitAtSale)

    public BillItem() { // No-arg constructor for frameworks
    }

    // Constructor for creating new BillItems (id, billId, billSerialNumber will be set later by repository)
    public BillItem(Item item, String itemNameAtSale, int quantityPurchased, BigDecimal pricePerUnitAtSale, BigDecimal totalPriceForItem) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null.");
        if (itemNameAtSale == null || itemNameAtSale.trim().isEmpty()) throw new IllegalArgumentException("Item name at sale cannot be null or empty.");
        // FIX: Allow 0 quantity for BillItem creation. Service methods will validate for positivity (quantity > 0)
        if (quantityPurchased < 0) throw new IllegalArgumentException("Quantity purchased cannot be negative.");
        if (pricePerUnitAtSale == null || pricePerUnitAtSale.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Price per unit at sale cannot be null or negative.");
        if (totalPriceForItem == null || totalPriceForItem.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Total price for item cannot be null or negative.");

        this.id = 0; // Will be set by DB
        this.billId = 0; // Will be set by parent bill
        this.billSerialNumber = null; // Will be set by parent bill
        this.item = item;
        this.itemNameAtSale = itemNameAtSale;
        this.quantityPurchased = quantityPurchased;
        this.pricePerUnitAtSale = pricePerUnitAtSale;
        this.totalPriceForItem = totalPriceForItem;
    }

    // Full constructor for loading from DB
    public BillItem(int id, int billId, String billSerialNumber, Item item, String itemNameAtSale, int quantityPurchased, BigDecimal pricePerUnitAtSale, BigDecimal totalPriceForItem) {
        // Reuse validation from simpler constructor, but without ID checks
        this(item, itemNameAtSale, quantityPurchased, pricePerUnitAtSale, totalPriceForItem);
        this.id = id;
        this.billId = billId;
        this.billSerialNumber = billSerialNumber;
    }

    // Getters
    public int getId() { return id; }
    public int getBillId() { return billId; }
    public String getBillSerialNumber() { return billSerialNumber; }
    public Item getItem() { return item; }
    public String getItemNameAtSale() { return itemNameAtSale; }
    public int getQuantityPurchased() { return quantityPurchased; }
    public BigDecimal getPricePerUnitAtSale() { return pricePerUnitAtSale; }
    public BigDecimal getTotalPriceForItem() { return totalPriceForItem; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setBillId(int billId) { this.billId = billId; }
    public void setBillSerialNumber(String billSerialNumber) { this.billSerialNumber = billSerialNumber; }
    public void setItem(Item item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null.");
        this.item = item;
    }
    public void setItemNameAtSale(String itemNameAtSale) {
        if (itemNameAtSale == null || itemNameAtSale.trim().isEmpty()) throw new IllegalArgumentException("Item name at sale cannot be null or empty.");
        this.itemNameAtSale = itemNameAtSale;
    }
    public void setQuantityPurchased(int quantityPurchased) {
        // FIX: Allow 0 quantity for BillItem (service will validate positivity for sales)
        if (quantityPurchased < 0) throw new IllegalArgumentException("Quantity purchased cannot be negative.");
        this.quantityPurchased = quantityPurchased;
        // Recalculate total price if quantity changes
        if (this.pricePerUnitAtSale != null) {
            this.totalPriceForItem = this.pricePerUnitAtSale.multiply(BigDecimal.valueOf(this.quantityPurchased));
        }
    }
    public void setPricePerUnitAtSale(BigDecimal pricePerUnitAtSale) {
        if (pricePerUnitAtSale == null || pricePerUnitAtSale.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Price per unit at sale cannot be null or negative.");
        this.pricePerUnitAtSale = pricePerUnitAtSale;
        // Recalculate total price if unit price changes
        this.totalPriceForItem = this.pricePerUnitAtSale.multiply(BigDecimal.valueOf(this.quantityPurchased));
    }
    public void setTotalPriceForItem(BigDecimal totalPriceForItem) {
        if (totalPriceForItem == null || totalPriceForItem.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Total price for item cannot be null or negative.");
        this.totalPriceForItem = totalPriceForItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BillItem billItem = (BillItem) o;
        if (id != 0 && billItem.id != 0) return id == billItem.id; // If IDs are present, use them
        return quantityPurchased == billItem.quantityPurchased &&
                Objects.equals(item != null ? item.getItemCode() : null, billItem.item != null ? billItem.item.getItemCode() : null) && // Compare by item code
                Objects.equals(itemNameAtSale, billItem.itemNameAtSale) &&
                Objects.equals(pricePerUnitAtSale, billItem.pricePerUnitAtSale);
    }

    @Override
    public int hashCode() {
        if (id != 0) return Objects.hash(id); // If ID is present, use it
        return Objects.hash(item != null ? item.getItemCode() : null, itemNameAtSale, quantityPurchased, pricePerUnitAtSale);
    }

    @Override
    public String toString() {
        return "BillItem{" +
                "id=" + id +
                ", billId=" + billId +
                ", billSerialNumber='" + billSerialNumber + '\'' +
                ", itemCode='" + (item != null ? item.getItemCode() : "null") + '\'' +
                ", itemNameAtSale='" + itemNameAtSale + '\'' +
                ", quantityPurchased=" + quantityPurchased +
                ", pricePerUnitAtSale=" + String.format("%.2f", pricePerUnitAtSale) +
                ", totalPriceForItem=" + String.format("%.2f", totalPriceForItem) +
                '}';
    }
}
