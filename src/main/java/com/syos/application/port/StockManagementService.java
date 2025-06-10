package com.syos.application.port;

import com.syos.domain.model.BillItem;
import com.syos.domain.model.StockBatch;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public interface StockManagementService {
    StockBatch addNewStockBatch(String itemCode, LocalDate purchaseDate, int quantityReceived, LocalDate expiryDate) throws ItemNotFoundException, DatabaseOperationException, IllegalArgumentException;
    List<StockBatch> getStockBatchesForItem(String itemCode) throws ItemNotFoundException, DatabaseOperationException;
    String moveStockToShelf(String itemCode, int quantityToMove) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException;
    String moveStockToWebsiteInventory(String itemCode, int quantityToMove) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException;
    void reduceStockAfterSale(List<BillItem> billItems) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException;
    void reduceWebsiteStockAfterOnlineSale(List<BillItem> billItems) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException;

    // CORRECTED: Removed ItemNotFoundException as it's not thrown by the implementation.
    void reduceWebsiteStockAfterOnlineSale(List<BillItem> billItems, Connection conn) throws InsufficientStockException, DatabaseOperationException;
    List<StockBatch> processAndRemoveExpiredStock() throws DatabaseOperationException;
}