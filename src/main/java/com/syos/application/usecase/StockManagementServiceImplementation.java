package com.syos.application.usecase;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.StockBatchRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ShelfStockRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.WebsiteStockRepository;

import com.syos.adapter.out.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.StockBatch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import com.syos.application.port.StockManagementService;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.model.BillItem;
import com.syos.domain.model.Item;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.model.WebsiteStock;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

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

    private StockBatch mapResultSetToStockBatch(ResultSet rs) throws SQLException {
        StockBatch batch = new StockBatch();
        batch.setBatchId(rs.getLong("id"));
        batch.setReceivedQuantity(rs.getInt("received_quantity"));
        batch.setCurrentQuantityInStore(rs.getInt("current_quantity_in_store"));
        batch.setCostPerUnit(rs.getBigDecimal("cost_per_unit"));
        batch.setReceivedDate(rs.getDate("received_date").toLocalDate());
        batch.setExpiryDate(rs.getDate("expiry_date").toLocalDate());

        // Optional: set item if needed
        // long itemId = rs.getLong("item_id");
        // batch.setItem(itemRepository.findById(itemId).orElse(null));

        return batch;
    }

    // --- Item Operations (Implementations for StockManagementService interface) ---

    @Override
    public Item addItem(Item item) throws DatabaseOperationException {
        if (item == null) {
            throw new IllegalArgumentException("Item to add cannot be null.");
        }
        System.out.println("Adding item: " + item.getName());

        try {
            return itemRepository.save(item); // assumes repository handles transactional save
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to add item: " + e.getMessage(), e);
        }
    }

    public Item registerNewItem(String itemCode, String name, String description, String category,
                                BigDecimal unitPrice, int reorderLevel, int reorderQuantity) throws DatabaseOperationException {

        // --- Basic Input Validation ---
        if (itemCode == null || itemCode.trim().isEmpty()) throw new IllegalArgumentException("Item code cannot be null or empty.");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Item name cannot be null or empty.");
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Unit price cannot be null or negative.");
        if (reorderLevel <= 0 || reorderQuantity <= 0) throw new IllegalArgumentException("Reorder level and reorder quantity must be positive.");
        if (category == null || category.trim().isEmpty()) throw new IllegalArgumentException("Category cannot be null or empty.");

        try {
            Optional<Item> existing = itemRepository.findByItemCode(itemCode);
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Item with code " + itemCode + " already exists.");
            }

            Item newItem = new Item(itemCode, name, description, category, unitPrice, reorderLevel, reorderQuantity);
            return itemRepository.save(newItem);

        } catch (DatabaseOperationException | IllegalArgumentException e) {
            // ✅ Concurrency/Validation: Propagate these specifically so they aren't wrapped
            throw e;
        } catch (Exception e) {
            // ✅ General Catch: Wrap only unexpected system failures
            throw new DatabaseOperationException("Failed to register new item: " + e.getMessage(), e);
        }
    }

    @Override
    public Item updateItem(Item item) throws DatabaseOperationException {
        if (item == null || item.getItemCode() == null) {
            throw new IllegalArgumentException("Item to update or its code cannot be null.");
        }
        System.out.println("Updating item: " + item.getItemCode());

        try {
            return itemRepository.update(item);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to update item: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteItem(String itemCode) throws ItemNotFoundException, DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Remove from shelf stock
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM shelf_stock WHERE item_code = ?")) {
                ps.setString(1, itemCode);
                ps.executeUpdate();
            }

            // Remove from website stock
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM website_stock WHERE item_code = ?")) {
                ps.setString(1, itemCode);
                ps.executeUpdate();
            }

            // Remove stock batches
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stock_batches WHERE item_code = ?")) {
                ps.setString(1, itemCode);
                ps.executeUpdate();
            }

            // Finally, remove the item itself
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE item_code = ?")) {
                ps.setString(1, itemCode);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    throw new ItemNotFoundException("Item not found: " + itemCode);
                }
            }

        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete item and related stock: " + e.getMessage(), e);
        }
    }



    @Override
    public Optional<Item> getItemByCode(String itemCode) throws DatabaseOperationException {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }

        try {
            return itemRepository.findByItemCode(itemCode);
        } catch (DatabaseOperationException e) {
            throw new DatabaseOperationException("Failed to retrieve item by code: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Item> getAllItems() throws DatabaseOperationException {
        try {
            return itemRepository.findAll();
        } catch (DatabaseOperationException e) {
            throw new DatabaseOperationException("Failed to retrieve all items: " + e.getMessage(), e);
        }
    }

    @Override
    public StockBatch receiveNewStockBatch(String itemCode, int quantity, LocalDate expiryDate, BigDecimal costPerUnit)
            throws ItemNotFoundException, DatabaseOperationException {

        // --- Validation ---
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Initial quantity must be positive.");
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date cannot be null.");
        }
        if (costPerUnit == null || costPerUnit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost per unit cannot be null or negative.");
        }

        // --- Retrieve Item ---
        Item item;
        try {
            item = itemRepository.findByItemCode(itemCode)
                    .orElseThrow(() -> new ItemNotFoundException(
                            "Cannot add stock batch. Item with code '" + itemCode + "' not found."
                    ));
        } catch (DatabaseOperationException e) {
            throw new DatabaseOperationException("Failed to retrieve item: " + e.getMessage(), e);
        }

        // --- Create Stock Batch ---
        StockBatch newBatch = new StockBatch();
        newBatch.setItem(item);
        newBatch.setReceivedQuantity(quantity);
        newBatch.setCurrentQuantityInStore(quantity);
        newBatch.setExpiryDate(expiryDate);
        newBatch.setCostPerUnit(costPerUnit);
        newBatch.setReceivedDate(LocalDate.now());

        // --- Save Batch ---
        try {
            return stockBatchRepository.save(newBatch);
        } catch (DatabaseOperationException e) {
            throw new DatabaseOperationException("Failed to save new stock batch: " + e.getMessage(), e);
        }
    }


    @Override
    public List<StockBatch> getAllStockBatches() throws DatabaseOperationException {
        return stockBatchRepository.findAll();
    }

    @Override
    public Optional<StockBatch> getStockBatchById(int batchId) throws DatabaseOperationException {
        return stockBatchRepository.findById((long) batchId); // Cast int to Long
    }

    @Override
    public List<StockBatch> getStockBatchesByItemCode(String itemCode) throws DatabaseOperationException {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        Optional<Item> itemOptional;
        try {
            itemOptional = itemRepository.findByItemCode(itemCode);
        } catch (DatabaseOperationException e) {
            throw new DatabaseOperationException("Failed to find item for stock batches: " + e.getMessage(), e);
        }
        if (itemOptional.isEmpty()) {
            return Collections.emptyList();
        }

        return stockBatchRepository.findByItem(itemOptional.get());
    }

    @Override
    public int deleteStockBatchesByItem(String itemCode) throws ItemNotFoundException, DatabaseOperationException {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // start transaction

            try {
                // Lock the item row to prevent concurrent operations on its stock
                Item item = itemRepository.findByItemCodeForUpdate(itemCode, conn)
                        .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemCode));

                // Lock all batches for this item
                List<StockBatch> batches = stockBatchRepository.findByItemForUpdate(item, conn);

                if (batches.isEmpty()) {
                    conn.commit();
                    return 0; // nothing to delete
                }

                // Delete all batches within the same transaction
                for (StockBatch batch : batches) {
                    stockBatchRepository.delete(batch, conn);
                }

                conn.commit(); // commit transaction
                return batches.size();

            } catch (Exception e) {
                conn.rollback(); // rollback if anything fails
                if (e instanceof ItemNotFoundException) throw (ItemNotFoundException) e;
                throw new DatabaseOperationException("Failed to delete stock batches: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true); // reset autocommit
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database connection error: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteStockBatch(long batchId) throws ItemNotFoundException, DatabaseOperationException {
        if (batchId <= 0) {
            throw new IllegalArgumentException("Batch ID must be positive.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Fetch batch for update to prevent concurrent modifications
                StockBatch batch = stockBatchRepository.findByIdForUpdate(batchId, conn)
                        .orElseThrow(() -> new ItemNotFoundException("Stock batch not found with ID: " + batchId));

                // Delete the batch
                stockBatchRepository.delete(batch, conn);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ItemNotFoundException) throw (ItemNotFoundException) e;
                throw new DatabaseOperationException("Failed to delete stock batch: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error while deleting stock batch: " + e.getMessage(), e);
        }
    }



    // --- Shelf Stock Operations ---

    @Override
    public ShelfStock addToShelf(String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add to shelf must be positive.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // start transaction

            Item item = itemRepository.findByItemCode(itemCode)
                    .orElseThrow(() -> new ItemNotFoundException("Item " + itemCode + " not found to add to shelf."));

            // Get batches (non-expired)
            List<StockBatch> batches = stockBatchRepository.findByItem(item);
            List<StockBatch> nonExpiredBatches = batches.stream()
                    .filter(batch -> !batch.getExpiryDate().isBefore(LocalDate.now()))
                    .sorted(Comparator.comparing(StockBatch::getExpiryDate)
                            .thenComparing(StockBatch::getReceivedDate))
                    .toList();

            int remainingToDeduct = quantity;

            for (StockBatch batch : nonExpiredBatches) {
                if (remainingToDeduct <= 0) break;

                int available = batch.getCurrentQuantityInStore();
                if (available >= remainingToDeduct) {
                    batch.setCurrentQuantityInStore(available - remainingToDeduct);
                    remainingToDeduct = 0;
                } else {
                    remainingToDeduct -= available;
                    batch.setCurrentQuantityInStore(0);
                }
                stockBatchRepository.save(batch);
            }

            if (remainingToDeduct > 0) {
                conn.rollback();
                throw new InsufficientStockException("Not enough stock batches available for item " + itemCode);
            }

            Optional<ShelfStock> existingShelfStock = shelfStockRepository.findByItem(item);
            ShelfStock shelfStock = existingShelfStock.orElse(new ShelfStock(item, 0));
            shelfStock.addQuantity(quantity);
            ShelfStock savedShelfStock = shelfStockRepository.save(shelfStock);

            conn.commit(); // commit transaction
            return savedShelfStock;

        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to add to shelf: " + e.getMessage(), e);
        }
    }


    @Override
    public ShelfStock removeFromShelf(String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove from shelf must be positive.");
        }

        try (Connection conn = com.syos.adapter.out.util.DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // start transaction

            try {
                // Fetch item
                Item item = itemRepository.findByItemCode(itemCode)
                        .orElseThrow(() -> new ItemNotFoundException(
                                "Item " + itemCode + " not found to remove from shelf."));

                // Fetch shelf stock
                Optional<ShelfStock> existingShelfStock = shelfStockRepository.findByItem(item);
                if (existingShelfStock.isEmpty()) {
                    throw new InsufficientStockException(
                            "No shelf stock found for item " + itemCode + " to remove.");
                }

                ShelfStock shelfStock = existingShelfStock.get();
                if (shelfStock.getQuantityOnShelf() < quantity) {
                    throw new InsufficientStockException(
                            "Insufficient quantity (" + shelfStock.getQuantityOnShelf() +
                                    ") on shelf for item " + itemCode + ". Requested: " + quantity);
                }

                // Reduce quantity
                shelfStock.reduceQuantity(quantity);

                // ✅ If quantity reaches 0, delete the row; otherwise, save
                if (shelfStock.getQuantityOnShelf() <= 0) {
                    shelfStockRepository.delete(shelfStock, conn);
                } else {
                    shelfStockRepository.save(shelfStock, conn);
                }

                conn.commit(); // commit transaction
                return shelfStock;

            } catch (Exception e) {
                // Rollback in case of any error
                try { conn.rollback(); } catch (SQLException ex) { /* log if needed */ }

                if (e instanceof DatabaseOperationException) throw (DatabaseOperationException) e;
                if (e instanceof InsufficientStockException) throw (InsufficientStockException) e;
                if (e instanceof ItemNotFoundException) throw (ItemNotFoundException) e;

                throw new DatabaseOperationException(
                        "Failed to remove stock from shelf: " + e.getMessage(), e);
            }

        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error while removing from shelf: " + e.getMessage(), e);
        }
    }

    @Override
    public void returnStockToShelf(String itemCode, int quantity)
            throws ItemNotFoundException, DatabaseOperationException {

        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to return must be positive.");
        }

        try (Connection conn = com.syos.adapter.out.util.DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // start transaction

            try {
                // Fetch the item
                Item item = itemRepository.findByItemCode(itemCode)
                        .orElseThrow(() -> new ItemNotFoundException(
                                "Item with code '" + itemCode + "' not found."));

                // Fetch existing shelf stock
                Optional<ShelfStock> existingShelfStock = shelfStockRepository.findByItem(item);
                ShelfStock shelfStock;

                if (existingShelfStock.isPresent()) {
                    shelfStock = existingShelfStock.get();
                    shelfStock.addQuantity(quantity);
                } else {
                    shelfStock = new ShelfStock(item, quantity);
                }

                // Save the updated shelf stock
                shelfStockRepository.save(shelfStock, conn);

                conn.commit(); // commit transaction

            } catch (Exception e) {
                try { conn.rollback(); } catch (SQLException ex) { /* log if needed */ }

                if (e instanceof DatabaseOperationException) throw (DatabaseOperationException) e;
                if (e instanceof ItemNotFoundException) throw (ItemNotFoundException) e;

                throw new DatabaseOperationException(
                        "Failed to return stock to shelf: " + e.getMessage(), e);
            }

        } catch (SQLException e) {
            throw new DatabaseOperationException(
                    "Database error while returning stock to shelf: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ShelfStock> getShelfStockByItemCode(String itemCode) throws DatabaseOperationException {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }

        final Optional<Item> itemOptional = itemRepository.findByItemCode(itemCode);
        if (itemOptional.isEmpty()) {
            return Optional.empty();
        }

        return shelfStockRepository.findByItem(itemOptional.get());
    }

    @Override
    public List<ShelfStock> getAllShelfStock() throws DatabaseOperationException {
        return shelfStockRepository.findAll();
    }

    @Override
    public int getAvailableShelfStock(String itemCode) throws DatabaseOperationException, ItemNotFoundException {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }

        final Item item = itemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new ItemNotFoundException("Item with code '" + itemCode + "' not found."));

        int totalAvailable = shelfStockRepository.findByItem(item)
                .map(ShelfStock::getQuantityOnShelf)
                .orElse(0);

        totalAvailable += stockBatchRepository.findByItem(item).stream()
                .filter(batch -> !batch.getExpiryDate().isBefore(LocalDate.now()))
                .mapToInt(StockBatch::getCurrentQuantityInStore)
                .sum();

        return totalAvailable;
    }



    // --- Website Stock Operations ---

    @Override
    public WebsiteStock addToWebsiteStock(String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add to website stock must be positive.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // start transaction

            try {
                // Retrieve item
                Item item = itemRepository.findByItemCode(itemCode)
                        .orElseThrow(() -> new ItemNotFoundException("Item " + itemCode + " not found to add to website stock."));

                // Lock stock batches (row-level lock)
                List<StockBatch> batches = stockBatchRepository.findByItem(item);
                List<StockBatch> nonExpiredBatches = batches.stream()
                        .filter(batch -> !batch.getExpiryDate().isBefore(LocalDate.now()))
                        .sorted(Comparator.comparing(StockBatch::getExpiryDate)
                                .thenComparing(StockBatch::getReceivedDate))
                        .toList();

                int remainingToDeduct = quantity;

                for (StockBatch batch : nonExpiredBatches) {
                    if (remainingToDeduct <= 0) break;

                    int available = batch.getCurrentQuantityInStore();
                    if (available >= remainingToDeduct) {
                        batch.setCurrentQuantityInStore(available - remainingToDeduct);
                        remainingToDeduct = 0;
                    } else {
                        remainingToDeduct -= available;
                        batch.setCurrentQuantityInStore(0);
                    }

                    // save each batch in the same transaction
                    stockBatchRepository.save(batch, conn);
                }

                if (remainingToDeduct > 0) {
                    conn.rollback();
                    throw new InsufficientStockException("Not enough stock batches available for item " + itemCode);
                }

                //  Update website stock in the same transaction
                Optional<WebsiteStock> existingWebsiteStock = websiteStockRepository.findByItem(item);
                WebsiteStock websiteStock = existingWebsiteStock.orElse(new WebsiteStock(item, 0));
                websiteStock.addQuantity(quantity);
                WebsiteStock savedWebsiteStock = websiteStockRepository.save(websiteStock, conn);

                conn.commit(); // commit transaction
                return savedWebsiteStock;

            } catch (Exception e) {
                conn.rollback(); // rollback on error
                if (e instanceof DatabaseOperationException) throw (DatabaseOperationException) e;
                if (e instanceof InsufficientStockException) throw (InsufficientStockException) e;
                if (e instanceof ItemNotFoundException) throw (ItemNotFoundException) e;
                throw new DatabaseOperationException("Failed to add to website stock: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database connection error: " + e.getMessage(), e);
        }
    }

    @Override
    public WebsiteStock removeFromWebsiteStock(String itemCode, int quantity)
            throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {

        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove from website stock must be positive.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // start transaction
            try {
                // Lock the item row to prevent concurrent modifications
                Item item = itemRepository.findByItemCodeForUpdate(itemCode, conn)
                        .orElseThrow(() -> new ItemNotFoundException(
                                "Item " + itemCode + " not found to remove from website stock."));

                Optional<WebsiteStock> existingWebsiteStock = websiteStockRepository.findByItemForUpdate(item, conn);
                if (existingWebsiteStock.isEmpty()) {
                    throw new InsufficientStockException(
                            "No website stock found for item " + itemCode + " to remove.");
                }

                WebsiteStock websiteStock = existingWebsiteStock.get();
                if (websiteStock.getQuantityAvailableOnline() < quantity) {
                    throw new InsufficientStockException(
                            "Insufficient quantity (" + websiteStock.getQuantityAvailableOnline() + ") on website for item " + itemCode + ". Requested: " + quantity);
                }

                // Reduce quantity
                websiteStock.reduceQuantity(quantity);

                // ✅ If quantity reaches 0, delete the row; otherwise, save
                if (websiteStock.getQuantityAvailableOnline() <= 0) {
                    websiteStockRepository.delete(websiteStock, conn);
                } else {
                    websiteStockRepository.save(websiteStock, conn);
                }

                conn.commit(); // commit transaction
                return websiteStock;

            } catch (Exception e) {
                conn.rollback(); // rollback if anything fails
                if (e instanceof ItemNotFoundException || e instanceof InsufficientStockException) {
                    throw e;
                }
                throw new DatabaseOperationException("Failed to remove stock from website: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true); // reset autocommit
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error during website stock removal: " + e.getMessage(), e);
        }
    }


    @Override
    public Optional<WebsiteStock> getWebsiteStockByItemCode(String itemCode) throws DatabaseOperationException {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Use the ForUpdate method for row-level locking
            Optional<Item> itemOptional = itemRepository.findByItemCodeForUpdate(itemCode, conn);
            if (itemOptional.isEmpty()) return Optional.empty();

            return websiteStockRepository.findByItemForUpdate(itemOptional.get(), conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error fetching website stock: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WebsiteStock> getAllWebsiteStock() throws DatabaseOperationException {

        return websiteStockRepository.findAll();
    }


    @Override
    public int getAvailableWebsiteStock(String itemCode) throws DatabaseOperationException, ItemNotFoundException {
        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Lock the item row
            Item item = itemRepository.findByItemCodeForUpdate(itemCode, conn)
                    .orElseThrow(() -> new ItemNotFoundException("Item with code '" + itemCode + "' not found."));

            // Lock the website stock row
            Optional<WebsiteStock> websiteStockOptional = websiteStockRepository.findByItemForUpdate(item, conn);

            return websiteStockOptional.map(WebsiteStock::getQuantityAvailableOnline).orElse(0);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error getting website stock: " + e.getMessage(), e);
        }
    }

    // --- Stock Reduction after Sales ---


    private void reduceShelfStockAfterPOSSaleSingleItem(String itemCode, int quantitySold)
            throws InsufficientStockException, DatabaseOperationException, ItemNotFoundException {

        if (itemCode == null || itemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Item code cannot be null or empty.");
        }
        if (quantitySold <= 0) {
            throw new IllegalArgumentException("Quantity sold must be positive.");
        }

        int quantityToDeduct = quantitySold;

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);

            // Lock the Item row
            Item item = itemRepository.findByItemCodeForUpdate(itemCode, conn)
                    .orElseThrow(() -> new ItemNotFoundException("Item with code '" + itemCode + "' not found."));

            // Lock ShelfStock row if it exists
            Optional<ShelfStock> shelfStockOptional = shelfStockRepository.findByItemForUpdate(item, conn);
            ShelfStock shelfStock = shelfStockOptional.orElse(null);

            // Deduct from ShelfStock first
            if (shelfStock != null) {
                int availableOnShelf = shelfStock.getQuantityOnShelf();
                if (availableOnShelf >= quantityToDeduct) {
                    shelfStock.reduceQuantity(quantityToDeduct);
                    shelfStockRepository.save(shelfStock, conn);
                    quantityToDeduct = 0;
                } else {
                    quantityToDeduct -= availableOnShelf;
                    shelfStock.reduceQuantity(availableOnShelf);
                    shelfStockRepository.save(shelfStock, conn);
                }
            }

            // Deduct from StockBatches (FIFO, non-expired)
            if (quantityToDeduct > 0) {
                List<StockBatch> batches = stockBatchRepository.findByItemForUpdate(item, conn);

                List<StockBatch> nonExpiredBatches = batches.stream()
                        .filter(batch -> !batch.getExpiryDate().isBefore(LocalDate.now()))
                        .sorted(Comparator
                                .comparing(StockBatch::getExpiryDate)
                                .thenComparing(StockBatch::getReceivedDate))
                        .toList();

                int currentTotalUnexpiredBatchStock = nonExpiredBatches.stream()
                        .mapToInt(StockBatch::getCurrentQuantityInStore)
                        .sum();

                if (quantityToDeduct > currentTotalUnexpiredBatchStock) {
                    int totalAvailableOverall;
                    try {
                        totalAvailableOverall = getAvailableShelfStock(itemCode);
                    } catch (ItemNotFoundException | DatabaseOperationException e) {
                        totalAvailableOverall = 0;
                    }
                    throw new InsufficientStockException("Insufficient stock for item " + itemCode +
                            ". Available: " + totalAvailableOverall + ", Requested: " + quantitySold);
                }

                for (StockBatch batch : nonExpiredBatches) {
                    if (quantityToDeduct <= 0) break;

                    int availableInBatch = batch.getCurrentQuantityInStore();
                    if (availableInBatch >= quantityToDeduct) {
                        batch.setCurrentQuantityInStore(availableInBatch - quantityToDeduct);
                        quantityToDeduct = 0;
                    } else {
                        quantityToDeduct -= availableInBatch;
                        batch.setCurrentQuantityInStore(0);
                    }

                    if (batch.getCurrentQuantityInStore() == 0) {
                        stockBatchRepository.delete(batch, conn);
                    } else {
                        stockBatchRepository.save(batch, conn);
                    }
                }
            }

            if (quantityToDeduct > 0) {
                throw new InsufficientStockException("An unexpected stock deduction issue occurred for item " + itemCode +
                        ". Remaining quantity to deduct: " + quantityToDeduct);
            }

            // Commit transaction
            conn.commit();

        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error during stock deduction for item " + itemCode + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void reduceShelfStockAfterPOSSale(List<BillItem> billItems)
            throws InsufficientStockException, DatabaseOperationException, ItemNotFoundException {

        if (billItems == null || billItems.isEmpty()) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (BillItem billItem : billItems) {

                    String itemCode = billItem.getItem().getItemCode();
                    int quantityToDeduct = billItem.getQuantityPurchased();

                    if (quantityToDeduct <= 0) {
                        throw new IllegalArgumentException("Quantity sold must be positive for item " + itemCode);
                    }

                    //  1. Lock ITEM row
                    Item item = itemRepository.findByItemCodeForUpdate(itemCode, conn)
                            .orElseThrow(() ->
                                    new ItemNotFoundException("Item not found: " + itemCode));

                    //  2. Lock SHELF STOCK row (if exists)
                    ShelfStock shelfStock =
                            shelfStockRepository.findByItemForUpdate(item, conn).orElse(null);

                    // 3. Deduct from shelf stock FIRST
                    if (shelfStock != null && quantityToDeduct > 0) {
                        int availableOnShelf = shelfStock.getQuantityOnShelf();
                        int deduct = Math.min(availableOnShelf, quantityToDeduct);

                        shelfStock.reduceQuantity(deduct);
                        shelfStockRepository.save(shelfStock, conn);

                        quantityToDeduct -= deduct;
                    }

                    // 4️. Deduct from STOCK BATCHES (FIFO, NON-EXPIRED)
                    if (quantityToDeduct > 0) {

                        //  Lock ALL batches for this item
                        List<StockBatch> batches =
                                stockBatchRepository.findByItemForUpdate(item, conn);

                        List<StockBatch> fifoBatches = batches.stream()
                                .filter(b -> !b.getExpiryDate().isBefore(LocalDate.now()))
                                .sorted(Comparator
                                        .comparing(StockBatch::getExpiryDate)
                                        .thenComparing(StockBatch::getReceivedDate))
                                .toList();

                        int totalAvailable = fifoBatches.stream()
                                .mapToInt(StockBatch::getCurrentQuantityInStore)
                                .sum();

                        if (quantityToDeduct > totalAvailable) {
                            throw new InsufficientStockException(
                                    "Insufficient stock for item " + itemCode +
                                            ". Available: " + totalAvailable +
                                            ", Requested: " + billItem.getQuantityPurchased());
                        }

                        for (StockBatch batch : fifoBatches) {
                            if (quantityToDeduct <= 0) break;

                            int availableInBatch = batch.getCurrentQuantityInStore();
                            int deduct = Math.min(availableInBatch, quantityToDeduct);

                            batch.setCurrentQuantityInStore(availableInBatch - deduct);
                            quantityToDeduct -= deduct;

                            if (batch.getCurrentQuantityInStore() == 0) {
                                stockBatchRepository.delete(batch, conn);
                            } else {
                                stockBatchRepository.save(batch, conn);
                            }
                        }
                    }

                    // 5️. Safety check (should never happen)
                    if (quantityToDeduct > 0) {
                        throw new InsufficientStockException(
                                "Unexpected stock deduction failure for item " + itemCode);
                    }
                }

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new DatabaseOperationException(
                    "Database error during POS stock reduction: " + e.getMessage(), e);
        }
    }



    @Override
    public void reduceWebsiteStockAfterOnlineSale(List<BillItem> billItems) throws InsufficientStockException, ItemNotFoundException, DatabaseOperationException {
        if (billItems == null) throw new IllegalArgumentException("Bill items list cannot be null.");
        if (billItems.isEmpty()) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (BillItem billItem : billItems) {
                    if (billItem == null || billItem.getItem() == null || billItem.getItem().getItemCode() == null || billItem.getItem().getItemCode().trim().isEmpty()) {
                        throw new IllegalArgumentException("Bill item or item code cannot be null or empty.");
                    }
                    if (billItem.getQuantityPurchased() <= 0) {
                        throw new IllegalArgumentException("Quantity for bill item " + billItem.getItem().getItemCode() + " must be positive.");
                    }

                    String itemCode = billItem.getItem().getItemCode();
                    int quantityToDeduct = billItem.getQuantityPurchased();

                    Item item = itemRepository.findByItemCodeForUpdate(itemCode, conn)
                            .orElseThrow(() -> new ItemNotFoundException("Item " + itemCode + " not found for website stock reduction."));

                    WebsiteStock websiteStock = websiteStockRepository.findByItemForUpdate(item, conn)
                            .orElseThrow(() -> new InsufficientStockException("No website stock found for item: " + itemCode));

                    if (websiteStock.getQuantityAvailableOnline() < quantityToDeduct) {
                        throw new InsufficientStockException("Insufficient website stock for item " + itemCode +
                                ". Available: " + websiteStock.getQuantityAvailableOnline() + ", Requested: " + quantityToDeduct);
                    }

                    websiteStock.reduceQuantity(quantityToDeduct);
                    websiteStockRepository.save(websiteStock, conn);
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error during website stock reduction: " + e.getMessage(), e);
        }
    }

    public List<StockBatch> findExpiredBatchesForUpdate(LocalDate currentDate, Connection conn) throws DatabaseOperationException {
        List<StockBatch> expiredBatches = new ArrayList<>();
        String sql = "SELECT * FROM stock_batches WHERE expiry_date < ? FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(currentDate)); // converts LocalDate to SQL Date
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                StockBatch batch = mapResultSetToStockBatch(rs); // calls private helper
                expiredBatches.add(batch);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch expired stock batches: " + e.getMessage(), e);
        }

        return expiredBatches;
    }



    //Expired Stock Processing

    @Override
    public List<StockBatch> processAndRemoveExpiredStock() throws DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Begin transaction
            conn.setAutoCommit(false);
            try {
                LocalDate currentDate = LocalDate.now();

                // Lock expired batches for update to prevent concurrent modifications
                List<StockBatch> expiredBatches = stockBatchRepository.findExpiredBatchesForUpdate(currentDate, conn);

                for (StockBatch batch : expiredBatches) {
                    stockBatchRepository.delete(batch, conn);
                }

                conn.commit();
                return expiredBatches;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database error during processing expired stock: " + e.getMessage(), e);
        }
    }


}