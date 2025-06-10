package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.ShelfStock;
import com.syos.domain.exception.DatabaseOperationException;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ShelfStockRepository {
    ShelfStock saveOrUpdate(ShelfStock shelfStock) throws DatabaseOperationException;
    Optional<ShelfStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException;
    List<ShelfStock> findByItemCode(String itemCode) throws DatabaseOperationException;
    ShelfStock update(ShelfStock shelfStock) throws DatabaseOperationException;
    void delete(ShelfStock shelfStock) throws DatabaseOperationException;

    // --- Methods for Service-Layer Transactions ---
    ShelfStock update(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException;
    void delete(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException;
}