package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.Item;
import com.syos.domain.model.StockBatch;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockBatchRepository {

    // --- Original methods (unchanged) ---
    StockBatch save(StockBatch batch) throws DatabaseOperationException;

    Optional<StockBatch> findById(Long batchId) throws DatabaseOperationException;

    List<StockBatch> findByItem(Item item) throws DatabaseOperationException;

    List<StockBatch> findAll() throws DatabaseOperationException;

    StockBatch update(StockBatch batch) throws DatabaseOperationException;

    List<StockBatch> findExpiredBatches(LocalDate currentDate) throws DatabaseOperationException;

    void delete(StockBatch stockBatch) throws DatabaseOperationException;

    int deleteBatchesById(List<Integer> batchIds) throws DatabaseOperationException;

    // --- New methods for concurrency-safe operations ---

    /**
     * Saves a StockBatch using an existing transaction (Connection).
     */
    StockBatch save(StockBatch batch, Connection conn) throws DatabaseOperationException;

    /**
     * Updates a StockBatch using an existing transaction (Connection).
     */
    StockBatch update(StockBatch batch, Connection conn) throws DatabaseOperationException;

    /**
     * Deletes a StockBatch using an existing transaction (Connection).
     */
    void delete(StockBatch batch, Connection conn) throws DatabaseOperationException;

    /**
     * Finds all stock batches for an item and locks them FOR UPDATE.
     * This ensures that two transactions cannot modify the same batch simultaneously.
     */
    List<StockBatch> findByItemForUpdate(Item item, Connection conn) throws DatabaseOperationException;

    /**
     * Finds all expired stock batches up to the given date and locks them FOR UPDATE
     * within the provided connection, to prevent concurrent deletion/modification.
     */
    List<StockBatch> findExpiredBatchesForUpdate(LocalDate currentDate, Connection conn) throws DatabaseOperationException;

    Optional<StockBatch> findByIdForUpdate(Long batchId, Connection conn) throws DatabaseOperationException;

}
