package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.WebsiteStock;
import com.syos.domain.model.Item;
import com.syos.domain.exception.DatabaseOperationException;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface WebsiteStockRepository {
    WebsiteStock save(WebsiteStock websiteStock) throws DatabaseOperationException;
    WebsiteStock save(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException;

    Optional<WebsiteStock> findById(Long id) throws DatabaseOperationException;
    Optional<WebsiteStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException;
    Optional<WebsiteStock> findByItemCode(String itemCode) throws DatabaseOperationException;
    Optional<WebsiteStock> findByItem(Item item) throws DatabaseOperationException; // Now Optional
    Optional<WebsiteStock> findByItemForUpdate(Item item, Connection conn) throws DatabaseOperationException;
    List<WebsiteStock> findAll() throws DatabaseOperationException;

    int getAvailableQuantity(String itemCode) throws DatabaseOperationException;

    WebsiteStock update(WebsiteStock websiteStock) throws DatabaseOperationException;
    WebsiteStock update(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException;

    void delete(Long id) throws DatabaseOperationException;
    void delete(WebsiteStock websiteStock) throws DatabaseOperationException;
    void delete(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException;

    /**
     * Deletes a WebsiteStock entry by its unique ID.
     * Added for consistency with ShelfStockRepository and to match implementation.
     * @param id The ID of the website stock entry to delete.
     * @throws DatabaseOperationException If a database error occurs.
     */
    void deleteById(Long id) throws DatabaseOperationException;
}
