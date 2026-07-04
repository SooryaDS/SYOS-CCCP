// com.syos.application.port.StockManagementService.java
package com.syos.application.port;

import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.model.BillItem;
import com.syos.domain.model.Item;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.model.StockBatch;
import com.syos.domain.model.WebsiteStock;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockManagementService {

    // --- Item Operations ---
    Item addItem(Item item) throws DatabaseOperationException;
    Item updateItem(Item item) throws DatabaseOperationException;
    void deleteItem(String itemCode) throws DatabaseOperationException, ItemNotFoundException;

    Optional<Item> getItemByCode(String itemCode) throws DatabaseOperationException;
    List<Item> getAllItems() throws DatabaseOperationException;

    // --- Stock Batch Operations ---
    StockBatch receiveNewStockBatch(String itemCode, int quantity, LocalDate expiryDate, BigDecimal costPerUnit)
            throws ItemNotFoundException, DatabaseOperationException;
    List<StockBatch> getAllStockBatches() throws DatabaseOperationException;
    Optional<StockBatch> getStockBatchById(int batchId) throws DatabaseOperationException;
    List<StockBatch> getStockBatchesByItemCode(String itemCode) throws DatabaseOperationException;
    /**
     * Deletes all stock batches for a given item code.
     *
     * @param itemCode The item code to delete batches for
     * @return number of batches deleted
     * @throws ItemNotFoundException if the item does not exist
     * @throws DatabaseOperationException on DB error
     */
    int deleteStockBatchesByItem(String itemCode) throws ItemNotFoundException, DatabaseOperationException;
    void deleteStockBatch(long batchId) throws ItemNotFoundException, DatabaseOperationException;


    // --- Shelf Stock Operations ---
    // Assuming ShelfStock is one entry per item, so find by itemCode should return Optional
    ShelfStock addToShelf(String itemCode, int quantity) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException;
    ShelfStock removeFromShelf(String itemCode, int quantity) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException;
    Optional<ShelfStock> getShelfStockByItemCode(String itemCode) throws DatabaseOperationException; // Changed to Optional
    List<ShelfStock> getAllShelfStock() throws DatabaseOperationException;
    int getAvailableShelfStock(String itemCode) throws DatabaseOperationException, ItemNotFoundException;
    void returnStockToShelf(String itemCode, int quantity) throws ItemNotFoundException, DatabaseOperationException; // Added this method

    // --- Website Stock Operations ---
    WebsiteStock addToWebsiteStock(String itemCode, int quantity) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException;
    WebsiteStock removeFromWebsiteStock(String itemCode, int quantity) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException;
    Optional<WebsiteStock> getWebsiteStockByItemCode(String itemCode) throws DatabaseOperationException; // Assuming Optional here too
    List<WebsiteStock> getAllWebsiteStock() throws DatabaseOperationException;
    int getAvailableWebsiteStock(String itemCode) throws DatabaseOperationException, ItemNotFoundException;

    // --- Stock Reduction after Sales ---
    void reduceShelfStockAfterPOSSale(List<BillItem> billItems) throws InsufficientStockException, DatabaseOperationException, ItemNotFoundException;
    void reduceWebsiteStockAfterOnlineSale(List<BillItem> billItems) throws InsufficientStockException, ItemNotFoundException, DatabaseOperationException;

    // --- Expired Stock Processing ---
    List<StockBatch> findExpiredBatchesForUpdate(LocalDate currentDate, Connection conn)
            throws DatabaseOperationException;

    List<StockBatch> processAndRemoveExpiredStock() throws DatabaseOperationException;
}