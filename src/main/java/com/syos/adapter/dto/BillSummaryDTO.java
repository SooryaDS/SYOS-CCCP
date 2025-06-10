package com.syos.adapter.dto;

import com.syos.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BillSummaryDTO {
    private final int billSerialNumber;
    private final LocalDateTime billDate;
    private final TransactionType transactionType;
    private final String customerIdentifier;
    private final BigDecimal finalTotalAmount;

    public BillSummaryDTO(int billSerialNumber, LocalDateTime billDate, TransactionType transactionType, String customerIdentifier, BigDecimal finalTotalAmount) {
        this.billSerialNumber = billSerialNumber;
        this.billDate = billDate;
        this.transactionType = transactionType;
        this.customerIdentifier = (customerIdentifier == null || customerIdentifier.trim().isEmpty()) ? "N/A" : customerIdentifier;
        this.finalTotalAmount = finalTotalAmount;
    }

    @Override
    public String toString() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("%-10d | %-20s | %-12s | %-15s | %10.2f", billSerialNumber, billDate.format(dateFormatter), transactionType.name(), customerIdentifier, finalTotalAmount);
    }
}