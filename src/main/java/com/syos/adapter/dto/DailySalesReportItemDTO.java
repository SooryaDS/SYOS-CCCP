package com.syos.adapter.dto;

import java.math.BigDecimal;

public class DailySalesReportItemDTO {
    private final String itemCode;
    private final String itemName;
    private final int totalQuantitySold;
    private final BigDecimal totalRevenueForItem;

    public DailySalesReportItemDTO(String itemCode, String itemName, int totalQuantitySold, BigDecimal totalRevenueForItem) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.totalQuantitySold = totalQuantitySold;
        this.totalRevenueForItem = totalRevenueForItem;
    }

    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public int getTotalQuantitySold() { return totalQuantitySold; }
    public BigDecimal getTotalRevenueForItem() { return totalRevenueForItem; }

    @Override
    public String toString() {
        return String.format("%-10s | %-25s | %10d | %15.2f", itemCode, itemName, totalQuantitySold, totalRevenueForItem);
    }
}