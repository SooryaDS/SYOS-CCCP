package com.syos.adapter.dto;

public class ReshelvingNeedsItemDTO {
    private String itemCode;
    private String itemName;
    private int currentShelfQuantity;
    private int minShelfStockThreshold;
    private int quantityToReshelve;

    public ReshelvingNeedsItemDTO(String itemCode, String itemName, int currentShelfQuantity, int minShelfStockThreshold) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.currentShelfQuantity = currentShelfQuantity;
        this.minShelfStockThreshold = minShelfStockThreshold;
        this.quantityToReshelve = Math.max(0, minShelfStockThreshold - currentShelfQuantity);
    }

    // Getters for all fields, including the missing getItemName()
    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() { // ADDED: Missing getter for itemName
        return itemName;
    }

    public int getCurrentShelfQuantity() {
        return currentShelfQuantity;
    }

    public int getMinShelfStockThreshold() {
        return minShelfStockThreshold;
    }

    public int getQuantityToReshelve() {
        return quantityToReshelve;
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-25s | %10d | %10d | %10d", itemCode, itemName, currentShelfQuantity, minShelfStockThreshold, quantityToReshelve);
    }
}
