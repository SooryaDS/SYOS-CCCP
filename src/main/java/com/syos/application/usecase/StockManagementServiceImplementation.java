package com.syos.application.usecase;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ShelfStockRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.StockBatchRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.WebsiteStockRepository;
import com.syos.application.port.StockManagementService;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.model.*;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StockManagementServiceImplementation implements StockManagementService {

    private final ItemRepository itemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ShelfStockRepository shelfStockRepository;
    private final WebsiteStockRepository websiteStockRepository;

    public StockManagementServiceImplementation(ItemRepository itemRepository,
                                                StockBatchRepository stockBatchRepository,
                                                ShelfStockRepository shelfStockRepository,
                                                WebsiteStockRepository websiteStockRepository) {
        this.itemRepository = itemRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.shelfStockRepository = shelfStockRepository;
        this.websiteStockRepository = websiteStockRepository;
    }

    @Override
    public StockBatch addNewStockBatch(String itemCode, LocalDate purchaseDate, int quantityReceived, LocalDate expiryDate) throws ItemNotFoundException, DatabaseOperationException, IllegalArgumentException {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be empty.");
        if (purchaseDate == null) throw new IllegalArgumentException("Purchase date cannot be null.");
        if (quantityReceived <= 0) throw new IllegalArgumentException("Quantity received must be positive.");

        Optional<Item> itemOptional = itemRepository.findByItemCode(itemCode);
        if (!itemOptional.isPresent()) {
            throw new ItemNotFoundException("Cannot add stock batch. Item with code '" + itemCode + "' not found.");
        }

        StockBatch newBatch = new StockBatch(itemCode, purchaseDate, quantityReceived, expiryDate);
        newBatch.setItem(itemOptional.get());
        return stockBatchRepository.save(newBatch);
    }

    @Override
    public List<StockBatch> getStockBatchesForItem(String itemCode) throws ItemNotFoundException, DatabaseOperationException {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be empty.");
        if (!itemRepository.findByItemCode(itemCode).isPresent()) {
            throw new ItemNotFoundException("Item with code '" + itemCode + "' not found.");
        }
        return stockBatchRepository.findByItemCode(itemCode);
    }

    @Override
    public String moveStockToShelf(String itemCode, int quantityToMove) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be empty.");
        if (quantityToMove <= 0) throw new IllegalArgumentException("Quantity to move must be positive.");

        if (!itemRepository.findByItemCode(itemCode).isPresent()) {
            throw new ItemNotFoundException("Item with code '" + itemCode + "' not found. Cannot move stock.");
        }

        List<StockBatch> availableBatches = stockBatchRepository.findByItemCode(itemCode);
        availableBatches.sort(Comparator.comparing(StockBatch::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(StockBatch::getPurchaseDate));

        int totalAvailable = availableBatches.stream().mapToInt(StockBatch::getCurrentQuantityInStore).sum();
        if (totalAvailable < quantityToMove) {
            throw new InsufficientStockException("Not enough stock in main store for item: " + itemCode + ". Available: " + totalAvailable + ", Requested: " + quantityToMove);
        }

        int remainingQuantityToMove = quantityToMove;
        StringBuilder resultMessage = new StringBuilder("Stock movement to SHELF for item '").append(itemCode).append("':\n");

        for (StockBatch batch : availableBatches) {
            if (remainingQuantityToMove <= 0) break;
            int quantityFromThisBatch = Math.min(remainingQuantityToMove, batch.getCurrentQuantityInStore());

            batch.setCurrentQuantityInStore(batch.getCurrentQuantityInStore() - quantityFromThisBatch);
            stockBatchRepository.update(batch);

            ShelfStock stockToAddOrUpdate = new ShelfStock(itemCode, batch.getBatchId(), quantityFromThisBatch);
            shelfStockRepository.saveOrUpdate(stockToAddOrUpdate);

            remainingQuantityToMove -= quantityFromThisBatch;
            resultMessage.append(String.format("  - Moved %d unit(s) from batch #%d to shelf.\n", quantityFromThisBatch, batch.getBatchId()));
        }

        resultMessage.append(String.format("Successfully moved %d unit(s) of %s to shelf.\n", quantityToMove, itemCode));
        return resultMessage.toString();
    }

    @Override
    public String moveStockToWebsiteInventory(String itemCode, int quantityToMove) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException, IllegalArgumentException {
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be empty.");
        if (quantityToMove <= 0) throw new IllegalArgumentException("Quantity to move must be positive.");

        if (!itemRepository.findByItemCode(itemCode).isPresent()) {
            throw new ItemNotFoundException("Item with code '" + itemCode + "' not found. Cannot move stock to website.");
        }

        List<StockBatch> availableBatches = stockBatchRepository.findByItemCode(itemCode);
        availableBatches.sort(Comparator.comparing(StockBatch::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(StockBatch::getPurchaseDate));

        int totalAvailable = availableBatches.stream().mapToInt(StockBatch::getCurrentQuantityInStore).sum();
        if (totalAvailable < quantityToMove) {
            throw new InsufficientStockException("No stock available in the main store for item: " + itemCode + ". Available: " + totalAvailable + ", Requested: " + quantityToMove);
        }

        int remainingQuantityToMove = quantityToMove;
        StringBuilder resultMessage = new StringBuilder("Stock movement to WEBSITE for item '").append(itemCode).append("':\n");

        for (StockBatch batch : availableBatches) {
            if (remainingQuantityToMove <= 0) break;
            int quantityFromThisBatch = Math.min(remainingQuantityToMove, batch.getCurrentQuantityInStore());

            batch.setCurrentQuantityInStore(batch.getCurrentQuantityInStore() - quantityFromThisBatch);
            stockBatchRepository.update(batch);

            WebsiteStock websiteStockEntry = new WebsiteStock(itemCode, batch.getBatchId(), quantityFromThisBatch);
            websiteStockRepository.saveOrUpdate(websiteStockEntry);

            remainingQuantityToMove -= quantityFromThisBatch;
            resultMessage.append(String.format("  - Moved %d unit(s) from batch #%d to website inventory.\n", quantityFromThisBatch, batch.getBatchId()));
        }

        resultMessage.append(String.format("Successfully moved %d unit(s) of %s to website inventory.\n", quantityToMove, itemCode));
        return resultMessage.toString();
    }

    @Override
    public void reduceStockAfterSale(List<BillItem> billItems) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        if (billItems == null || billItems.isEmpty()) return;

        System.out.println("Processing SHELF stock reduction for POS sale...");
        for (BillItem billItem : billItems) {
            String itemCode = billItem.getItem().getItemCode();
            int quantitySold = billItem.getQuantityPurchased();
            int remainingToReduce = quantitySold;

            List<ShelfStock> shelfStocks = shelfStockRepository.findByItemCode(itemCode);
            int totalShelfStock = shelfStocks.stream().mapToInt(ShelfStock::getQuantityOnShelf).sum();

            if (totalShelfStock < quantitySold) {
                throw new InsufficientStockException("CRITICAL STOCK DISCREPANCY: Not enough stock on shelf for item " + itemCode + ". Required: " + quantitySold + ", Found: " + totalShelfStock);
            }

            for (ShelfStock currentShelfStock : shelfStocks) {
                if (remainingToReduce <= 0) break;

                int quantityToTake = Math.min(remainingToReduce, currentShelfStock.getQuantityOnShelf());

                if (quantityToTake > 0) {
                    currentShelfStock.setQuantityOnShelf(currentShelfStock.getQuantityOnShelf() - quantityToTake);
                    if (currentShelfStock.getQuantityOnShelf() == 0) {
                        shelfStockRepository.delete(currentShelfStock);
                    } else {
                        shelfStockRepository.update(currentShelfStock);
                    }
                    remainingToReduce -= quantityToTake;
                }
            }
        }
    }

    @Override
    public void reduceWebsiteStockAfterOnlineSale(List<BillItem> billItems) throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                reduceWebsiteStockAfterOnlineSale(billItems, conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof DatabaseOperationException) throw (DatabaseOperationException) e;
                if (e instanceof InsufficientStockException) throw (InsufficientStockException) e;
                if (e instanceof ItemNotFoundException) throw (ItemNotFoundException) e;
                throw new DatabaseOperationException("Transaction failed for website stock reduction.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to manage connection for website stock reduction", e);
        }
    }

    @Override
    public void reduceWebsiteStockAfterOnlineSale(List<BillItem> billItems, Connection conn) throws InsufficientStockException, DatabaseOperationException {
        if (billItems == null || billItems.isEmpty()) return;

        System.out.println("Processing WEBSITE stock reduction within transaction...");
        for (BillItem billItem : billItems) {
            String itemCode = billItem.getItem().getItemCode();
            int quantitySold = billItem.getQuantityPurchased();
            int remainingToReduce = quantitySold;

            List<WebsiteStock> stocks = websiteStockRepository.findByItemCode(itemCode);
            int totalWebsiteStock = stocks.stream().mapToInt(WebsiteStock::getQuantityAvailableOnline).sum();

            if(totalWebsiteStock < quantitySold){
                throw new InsufficientStockException("Not enough stock for item " + itemCode + ". Available: " + totalWebsiteStock + ", Required: " + quantitySold);
            }

            for (WebsiteStock stock : stocks) {
                if (remainingToReduce <= 0) break;
                int quantityToTake = Math.min(remainingToReduce, stock.getQuantityAvailableOnline());
                if (quantityToTake > 0) {
                    stock.setQuantityAvailableOnline(stock.getQuantityAvailableOnline() - quantityToTake);
                    if (stock.getQuantityAvailableOnline() == 0) {
                        websiteStockRepository.delete(stock, conn);
                    } else {
                        websiteStockRepository.update(stock, conn);
                    }
                    remainingToReduce -= quantityToTake;
                }
            }
            if (remainingToReduce > 0) {
                throw new InsufficientStockException("Logic error: Could not reduce enough stock for " + itemCode);
            }
        }
    }

    @Override
    public List<StockBatch> processAndRemoveExpiredStock() throws DatabaseOperationException {
        LocalDate today = LocalDate.now();
        List<StockBatch> expiredBatches = stockBatchRepository.findExpiredBatches(today);
        if (expiredBatches.isEmpty()) return new ArrayList<>();

        List<Integer> batchIdsToDelete = expiredBatches.stream()
                .map(StockBatch::getBatchId)
                .collect(Collectors.toList());

        stockBatchRepository.deleteBatchesById(batchIdsToDelete);
        return expiredBatches;
    }
}
