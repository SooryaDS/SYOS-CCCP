package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.StockBatch;
import com.syos.domain.exception.DatabaseOperationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockBatchRepository {
    StockBatch save(StockBatch batch) throws DatabaseOperationException;
    Optional<StockBatch> findById(int batchId) throws DatabaseOperationException;
    List<StockBatch> findByItemCode(String itemCode) throws DatabaseOperationException;
    List<StockBatch> findAll() throws DatabaseOperationException;
    StockBatch update(StockBatch batch) throws DatabaseOperationException;

    /**
     * Finds all stock batches that have expired as of the given date.
     * @param currentDate The current date to check against.
     * @return A list of expired StockBatch objects.
     * @throws DatabaseOperationException if a database error occurs.
     */
    List<StockBatch> findExpiredBatches(LocalDate currentDate) throws DatabaseOperationException;

    /**
     * Deletes a list of stock batches from the database, typically by their IDs.
     * @param batchIds The list of batch IDs to delete.
     * @return The number of rows deleted.
     * @throws DatabaseOperationException if a database error occurs.
     */
    int deleteBatchesById(List<Integer> batchIds) throws DatabaseOperationException;
}