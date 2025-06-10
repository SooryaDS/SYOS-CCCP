package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.Bill;
import com.syos.domain.enums.TransactionType;
import com.syos.adapter.dto.BillSummaryDTO;
import com.syos.adapter.dto.DailySalesReportItemDTO;
import com.syos.domain.exception.DatabaseOperationException;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository {
    int findNextBillSerialNumberForToday() throws DatabaseOperationException;
    Bill save(Bill bill) throws DatabaseOperationException;
    Bill save(Bill bill, Connection conn) throws DatabaseOperationException;
    Optional<Bill> findBySerialNumberAndDate(int billSerialNumber, LocalDate billDate) throws DatabaseOperationException;
    List<DailySalesReportItemDTO> findSalesDataByDateAndType(LocalDate date, TransactionType transactionType) throws DatabaseOperationException;
    List<BillSummaryDTO> findAllBillSummaries(LocalDate startDate, LocalDate endDate, TransactionType transactionType) throws DatabaseOperationException;
    Optional<Bill> findTopByBillDateOrderBySerialNumberDesc(LocalDate date) throws DatabaseOperationException;
}
