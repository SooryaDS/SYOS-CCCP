package com.syos.adapter.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BatchStockReportItemDTO {
    private String itemCode;
    private String itemName;
    private int batchId;
    private LocalDate purchaseDate;
    private int quantityReceived;
    private int currentQuantityInStore;
    private LocalDate expiryDate;

    public BatchStockReportItemDTO(String itemCode, String itemName, int batchId, LocalDate purchaseDate, int quantityReceived, int currentQuantityInStore, LocalDate expiryDate) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.batchId = batchId;
        this.purchaseDate = purchaseDate;
        this.quantityReceived = quantityReceived;
        this.currentQuantityInStore = currentQuantityInStore;
        this.expiryDate = expiryDate;
    }

    // Getters
    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public int getBatchId() {
        return batchId;
    }

    public LocalDate getPurchaseDate() { // ADDED: Missing getter for purchaseDate
        return purchaseDate;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public int getCurrentQuantityInStore() {
        return currentQuantityInStore;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    @Override
    public String toString() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        return String.format("%-10s | %-20s | %-8d | %-12s | %-10d | %-10d | %-12s", itemCode, itemName, batchId, purchaseDate.format(dateFormatter), quantityReceived, currentQuantityInStore, expiryDate != null ? expiryDate.format(dateFormatter) : "N/A");
    }
}
