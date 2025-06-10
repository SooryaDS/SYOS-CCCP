package com.syos.adapter.dto;

public class ReorderReportItemDTO {
    private String itemCode;
    private String itemName;
    private int currentTotalStock;
    private int reorderLevelThreshold;

    public ReorderReportItemDTO(String itemCode, String itemName, int currentTotalStock, int reorderLevelThreshold) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.currentTotalStock = currentTotalStock;
        this.reorderLevelThreshold = reorderLevelThreshold;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public int getCurrentTotalStock() {
        return currentTotalStock;
    }

    public int getReorderLevelThreshold() {
        return reorderLevelThreshold;
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-25s | %10d | %10d", itemCode, itemName, currentTotalStock, reorderLevelThreshold);
    }
}
