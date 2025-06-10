package com.syos.application.port;

import com.syos.domain.enums.TransactionType;
import com.syos.adapter.dto.BatchStockReportItemDTO;
import com.syos.adapter.dto.BillSummaryDTO; // New DTO
import com.syos.adapter.dto.DailySalesReportDTO;
import com.syos.adapter.dto.ReorderReportItemDTO;
import com.syos.adapter.dto.ReshelvingNeedsItemDTO;
import com.syos.domain.exception.DatabaseOperationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportingService {
    DailySalesReportDTO generateDailySalesReport(LocalDate date, Optional<TransactionType> transactionTypeFilter)
            throws DatabaseOperationException, IllegalArgumentException;

    List<ReorderReportItemDTO> generateReorderLevelReport() throws DatabaseOperationException;

    List<BatchStockReportItemDTO> generateBatchStockReport() throws DatabaseOperationException;

    List<ReshelvingNeedsItemDTO> generateDailyReshelvingNeedsReport() throws DatabaseOperationException;

    /**
     * Generates a report of all bill transactions, optionally filtered.
     * @param startDate Start date for filtering (inclusive, can be null).
     * @param endDate End date for filtering (inclusive, can be null).
     * @param transactionTypeFilter Optional filter by transaction type.
     * @return A list of BillSummaryDTO.
     * @throws DatabaseOperationException If a database error occurs.
     */
    List<BillSummaryDTO> generateBillTransactionReport(LocalDate startDate, LocalDate endDate, Optional<TransactionType> transactionTypeFilter)
            throws DatabaseOperationException;
}