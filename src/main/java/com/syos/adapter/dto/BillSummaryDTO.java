package com.syos.adapter.dto;

import com.syos.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BillSummaryDTO {
    private String billSerialNumber;
    private LocalDateTime billDate;
    private TransactionType transactionType;
    private Integer userId; // Changed from String to Integer
    private BigDecimal finalTotalAmount;

    public BillSummaryDTO(String billSerialNumber, LocalDateTime billDate, TransactionType transactionType, Integer userId, BigDecimal finalTotalAmount) {
        this.billSerialNumber = billSerialNumber;
        this.billDate = billDate;
        this.transactionType = transactionType;
        this.userId = userId;
        this.finalTotalAmount = finalTotalAmount;
    }

    // Getters for display purposes
    public String getBillSerialNumber() {
        return billSerialNumber;
    }

    public LocalDateTime getBillDate() {
        return billDate;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Integer getUserId() { // Changed return type to Integer
        return userId;
    }

    public BigDecimal getFinalTotalAmount() {
        return finalTotalAmount;
    }

    @Override
    public String toString() {
        return "BillSummaryDTO{" +
                "billSerialNumber='" + billSerialNumber + '\'' +
                ", billDate=" + billDate +
                ", transactionType=" + transactionType +
                ", userId=" + userId + // No quotes needed for Integer
                ", finalTotalAmount=" + finalTotalAmount +
                '}';
    }
}