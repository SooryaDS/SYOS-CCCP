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

    /**
     * Finds the next available daily serial number for a bill (YYYYMMDDNNNN format).
     * @return The next available bill serial number as a String.
     * @throws DatabaseOperationException If a database error occurs.
     */
    String findNextBillSerialNumberForToday() throws DatabaseOperationException;

    /**
     * Saves a Bill object to the database.
     * @param bill The Bill object to save.
     * @return The saved Bill object, potentially with its database-generated ID.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Bill save(Bill bill) throws DatabaseOperationException;

    /**
     * Saves a Bill object to the database within an existing connection (for transactions).
     * @param bill The Bill object to save.
     * @param conn The active SQL Connection.
     * @return The saved Bill object, potentially with its database-generated ID.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Bill save(Bill bill, Connection conn) throws DatabaseOperationException;

    /**
     * Finds a Bill by its internal database ID.
     * @param id The internal database ID of the bill.
     * @return An Optional containing the Bill if found, empty otherwise.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<Bill> findById(int id) throws DatabaseOperationException;

    /**
     * Finds a Bill by its human-readable serial number and bill date.
     * @param billSerialNumber The YYYYMMDDNNNN serial number.
     * @param billDate The date of the bill.
     * @return An Optional containing the Bill if found, empty otherwise.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<Bill> findBySerialNumberAndDate(String billSerialNumber, LocalDate billDate) throws DatabaseOperationException;

    /**
     * Retrieves daily sales report items for a specific date and transaction type.
     * @param date The date for the report.
     * @param transactionType The type of transaction to filter by (can be null for all types).
     * @return A list of DailySalesReportItemDTO.
     * @throws DatabaseOperationException If a database error occurs.
     */
    List<DailySalesReportItemDTO> findSalesDataByDateAndType(LocalDate date, TransactionType transactionType) throws DatabaseOperationException;

    /**
     * Retrieves summaries of bills within a date range and optionally by transaction type.
     * @param startDate The start date for the range.
     * @param endDate The end date for the range.
     * @param transactionType The type of transaction to filter by (can be null for all types).
     * @return A list of BillSummaryDTO.
     * @throws DatabaseOperationException If a database error occurs.
     */
    List<BillSummaryDTO> findAllBillSummaries(LocalDate startDate, LocalDate endDate, TransactionType transactionType) throws DatabaseOperationException;

    /**
     * Finds the bill with the highest serial number for a specific date.
     * Used internally for generating the next serial number.
     * @param billDate The date to search for.
     * @return An Optional containing the Bill if found, empty otherwise.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<Bill> findTopByBillDateOrderBySerialNumberDesc(LocalDate billDate) throws DatabaseOperationException;
}