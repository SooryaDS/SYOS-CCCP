package com.syos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class BillItem {
    private Item item;
    private String itemNameAtSale;
    private int quantityPurchased;
    private BigDecimal pricePerUnitAtSale;
    private BigDecimal totalPriceForItem;

    public BillItem(Item item, int quantityPurchased) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null for a bill line.");
        if (quantityPurchased <= 0) throw new IllegalArgumentException("Quantity purchased must be positive.");
        this.item = item;
        this.itemNameAtSale = item.getName();
        this.quantityPurchased = quantityPurchased;
        this.pricePerUnitAtSale = item.getUnitPrice();
        this.totalPriceForItem = this.pricePerUnitAtSale.multiply(BigDecimal.valueOf(quantityPurchased));
    }

    public BillItem(Item item, String itemNameAtSale, int quantityPurchased, BigDecimal pricePerUnitAtSale, BigDecimal totalPriceForItem) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null for a bill line.");
        if (quantityPurchased <= 0) throw new IllegalArgumentException("Quantity purchased must be positive.");
        if (pricePerUnitAtSale == null || totalPriceForItem == null) throw new IllegalArgumentException("Price per unit and total price for item cannot be null.");
        this.item = item;
        this.itemNameAtSale = itemNameAtSale;
        this.quantityPurchased = quantityPurchased;
        this.pricePerUnitAtSale = pricePerUnitAtSale;
        this.totalPriceForItem = totalPriceForItem;
    }

    public Item getItem() { return item; }
    public String getItemNameAtSale() { return itemNameAtSale; }
    public int getQuantityPurchased() { return quantityPurchased; }
    public BigDecimal getPricePerUnitAtSale() { return pricePerUnitAtSale; }
    public BigDecimal getTotalPriceForItem() { return totalPriceForItem; }

    public void setQuantityPurchased(int quantityPurchased) {
        if (quantityPurchased < 0) throw new IllegalArgumentException("Quantity purchased cannot be negative.");
        this.quantityPurchased = quantityPurchased;
        this.totalPriceForItem = this.pricePerUnitAtSale.multiply(BigDecimal.valueOf(quantityPurchased));
    }

    @Override
    public String toString() {
        return String.format("%-20s | %3d | %8.2f | %8.2f", getItemNameAtSale(), quantityPurchased, pricePerUnitAtSale, totalPriceForItem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BillItem billItem = (BillItem) o;
        return quantityPurchased == billItem.quantityPurchased && Objects.equals(item.getItemCode(), billItem.item.getItemCode()) && Objects.equals(pricePerUnitAtSale, billItem.pricePerUnitAtSale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item.getItemCode(), quantityPurchased, pricePerUnitAtSale);
    }
}