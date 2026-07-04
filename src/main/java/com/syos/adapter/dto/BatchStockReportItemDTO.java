package com.syos.adapter.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class BatchStockReportItemDTO {
    private String itemCode;
    private String itemName;
    private Long batchId;
    private LocalDate receivedDate;
    private int quantityReceived;
    private int currentQuantityInStore;
    private LocalDate expiryDate;


    public BatchStockReportItemDTO(String itemCode, String itemName, Long batchId, LocalDate receivedDate,
                                   int quantityReceived, int currentQuantityInStore, LocalDate expiryDate) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.batchId = batchId;
        this.receivedDate = receivedDate;
        this.quantityReceived = quantityReceived;
        this.currentQuantityInStore = currentQuantityInStore;
        this.expiryDate = expiryDate;
    }

    // --- Getters ---
    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public Long getBatchId() { // FIX: Returns Long
        return batchId;
    }

    public LocalDate getReceivedDate() { // FIX: Renamed from getPurchaseDate
        return receivedDate;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchStockReportItemDTO that = (BatchStockReportItemDTO) o;
        return Objects.equals(itemCode, that.itemCode) &&
                Objects.equals(batchId, that.batchId) &&
                Objects.equals(receivedDate, that.receivedDate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(itemCode, batchId, receivedDate);
    }

    @Override
    public String toString() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        return String.format("%-10s | %-20s | %-8s | %-12s | %-10d | %-10d | %-12s",
                itemCode, itemName, batchId.toString(),
                receivedDate != null ? receivedDate.format(dateFormatter) : "N/A",
                quantityReceived, currentQuantityInStore,
                expiryDate != null ? expiryDate.format(dateFormatter) : "N/A");
    }
}