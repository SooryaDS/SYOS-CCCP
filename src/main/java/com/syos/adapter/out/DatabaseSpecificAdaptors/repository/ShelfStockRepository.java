// com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ShelfStockRepository.java
package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.model.Item;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ShelfStockRepository {

    ShelfStock save(ShelfStock shelfStock) throws DatabaseOperationException;
    ShelfStock save(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException;

    Optional<ShelfStock> findById(Long id) throws DatabaseOperationException;

    // IMPORTANT: This is now Optional<ShelfStock> again to ensure consistency
    // ShelfStock represents the total quantity on shelf for a given Item.
    Optional<ShelfStock> findByItem(Item item) throws DatabaseOperationException;

    /**
     * Finds the ShelfStock row for a given Item with a row-level lock (FOR UPDATE)
     * using the provided JDBC Connection.
     * @param item The Item to look for.
     * @param conn The JDBC Connection to use.
     * @return Optional containing the ShelfStock if found, empty otherwise.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<ShelfStock> findByItemForUpdate(Item item, Connection conn) throws DatabaseOperationException;


    // This method is for finding by item code and batch ID.
    // Ensure your shelf_stock table has a 'batch_id' column if you use this.
    Optional<ShelfStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException;

    List<ShelfStock> findAll() throws DatabaseOperationException;

    int getAvailableQuantity(String itemCode) throws DatabaseOperationException;

    // Update methods can delegate to save, as save should handle both insert/update
    ShelfStock update(ShelfStock shelfStock) throws DatabaseOperationException;
    ShelfStock update(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException;

    void delete(Long id) throws DatabaseOperationException;
    void delete(ShelfStock shelfStock) throws DatabaseOperationException;
    void delete(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException;
    void deleteById(Long id) throws DatabaseOperationException; // Still needed based on your impl
}