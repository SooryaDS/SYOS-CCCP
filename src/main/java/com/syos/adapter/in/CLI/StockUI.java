package com.syos.adapter.in.CLI;

import com.syos.application.port.StockManagementService;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.model.Item;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.model.StockBatch;
import com.syos.domain.model.WebsiteStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.time.LocalDateTime;

public class StockUI {
    private final StockManagementService stockManagementService;
    private final Scanner scanner;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public StockUI(StockManagementService stockManagementService, Scanner scanner) {
        this.stockManagementService = stockManagementService;
        this.scanner = scanner;
    }

    public void displayStockMenu() {
        System.out.println("\n--- Stock Management ---");
        System.out.println("----------------------------------------");
        System.out.println("1. Add New Item");
        System.out.println("2. Update Item Details");
        System.out.println("3. Delete Item");
        System.out.println("4. Receive New Stock Batch");
        System.out.println("5. Move Stock to Shelf");
        System.out.println("6. Move Stock to Website");
        System.out.println("7. View All Items");
        System.out.println("8. View Stock Details by Item Code");
        System.out.println("9. View All Stock Batches");
        System.out.println("10. View All Shelf Stock");
        System.out.println("11. View All Website Stock");
        System.out.println("12. Process Expired Stock");
        System.out.println("----------------------------------------");
        System.out.println("0. Back to Main Menu");
        System.out.println("----------------------------------------");
    }

    public boolean handleStockAction(int choice) {
        try {
            switch (choice) {
                case 1:
                    addNewItem();
                    break;
                case 2:
                    updateItem();
                    break;
                case 3:
                    deleteItem();
                    break;
                case 4:
                    receiveNewStockBatch();
                    break;
                case 5:
                    moveStockToShelf();
                    break;
                case 6:
                    moveStockToWebsite();
                    break;
                case 7:
                    viewAllItems();
                    break;
                case 8:
                    viewStockDetailsByItemCode();
                    break;
                case 9:
                    viewAllStockBatches();
                    break;
                case 10:
                    viewAllShelfStock();
                    break;
                case 11:
                    viewAllWebsiteStock();
                    break;
                case 12:
                    processExpiredStock();
                    break;
                case 0:
                    return false;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Input Error: Invalid number format. Please enter a valid number.");
        } catch (DateTimeParseException e) {
            System.err.println("Input Error: Invalid date format. Please use YYYY-MM-DD.");
        } catch (ItemNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (InsufficientStockException e) {
            System.err.println("Stock Error: " + e.getMessage());
        } catch (DatabaseOperationException e) {
            System.err.println("Database Error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    private void addNewItem() throws DatabaseOperationException {
        System.out.println("\n--- Add New Item ---");
        System.out.print("Item Code (e.g., ABC001): "); String itemCode = scanner.nextLine().trim();
        System.out.print("Item Name: "); String name = scanner.nextLine().trim();
        System.out.print("Description (optional): "); String description = scanner.nextLine().trim();
        System.out.print("Category: "); String category = scanner.nextLine().trim();
        System.out.print("Unit Price (e.g., 10.50): "); BigDecimal unitPrice = new BigDecimal(scanner.nextLine().trim());
        System.out.print("Reorder Level: "); int reorderLevel = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Reorder Quantity: "); int reorderQuantity = Integer.parseInt(scanner.nextLine().trim());

        Item newItem = new Item(itemCode, name, description, category, unitPrice, reorderLevel, reorderQuantity);
        stockManagementService.addItem(newItem);
        System.out.println("Item added successfully.");
    }

    private void updateItem() throws DatabaseOperationException, ItemNotFoundException {
        System.out.println("\n--- Update Item Details ---");
        System.out.print("Item Code to Update: "); String itemCode = scanner.nextLine().trim();

        Optional<Item> existingItem = stockManagementService.getItemByCode(itemCode);
        if (existingItem.isEmpty()) throw new ItemNotFoundException("Item '" + itemCode + "' not found.");
        Item itemToUpdate = existingItem.get();

        System.out.println("Current Name: " + itemToUpdate.getName());
        System.out.print("New Name (leave blank to keep current): "); String newName = scanner.nextLine().trim();
        if (!newName.isEmpty()) itemToUpdate.setName(newName);

        System.out.println("Current Description: " + (itemToUpdate.getDescription() != null ? itemToUpdate.getDescription() : "N/A"));
        System.out.print("New Description (leave blank to keep current): "); String newDescription = scanner.nextLine().trim();
        if (!newDescription.isEmpty()) itemToUpdate.setDescription(newDescription);

        System.out.println("Current Category: " + (itemToUpdate.getCategory() != null ? itemToUpdate.getCategory() : "N/A"));
        System.out.print("New Category (leave blank to keep current): "); String newCategory = scanner.nextLine().trim();
        if (!newCategory.isEmpty()) itemToUpdate.setCategory(newCategory);

        System.out.println("Current Unit Price: " + itemToUpdate.getUnitPrice());
        System.out.print("New Unit Price (leave blank to keep current): "); String newPriceStr = scanner.nextLine().trim();
        if (!newPriceStr.isEmpty()) itemToUpdate.setUnitPrice(new BigDecimal(newPriceStr));

        System.out.println("Current Reorder Level: " + itemToUpdate.getReorderLevel());
        System.out.print("New Reorder Level (leave blank to keep current): "); String newReorderStr = scanner.nextLine().trim();
        if (!newReorderStr.isEmpty()) itemToUpdate.setReorderLevel(Integer.parseInt(newReorderStr));

        System.out.println("Current Reorder Quantity: " + itemToUpdate.getReorderQuantity());
        System.out.print("New Reorder Quantity (leave blank to keep current): "); String newReorderQtyStr = scanner.nextLine().trim();
        if (!newReorderQtyStr.isEmpty()) itemToUpdate.setReorderQuantity(Integer.parseInt(newReorderQtyStr));

        stockManagementService.updateItem(itemToUpdate);
        System.out.println("Item updated successfully.");
    }

    private void deleteItem() throws DatabaseOperationException {
        System.out.println("\n--- Delete Item ---");
        System.out.print("Item Code to Delete: ");
        String itemCode = scanner.nextLine().trim();

        Optional<Item> itemOptional = stockManagementService.getItemByCode(itemCode);
        if (itemOptional.isEmpty()) {
            System.out.println("Item '" + itemCode + "' not found.");
            return;
        }

        System.out.print("Confirm delete '" + itemOptional.get().getName() + "' (" + itemCode + ")? (yes/no): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
            // Only catch DatabaseOperationException here
            try {
                stockManagementService.deleteItem(itemCode);
                System.out.println("Item deleted successfully.");
            } catch (DatabaseOperationException | ItemNotFoundException e) {
                System.err.println("Failed to delete item: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Item deletion cancelled.");
        }
    }

    private void receiveNewStockBatch() throws ItemNotFoundException, DatabaseOperationException {
        System.out.println("\n--- Receive New Stock Batch ---");
        System.out.print("Item Code: "); String itemCode = scanner.nextLine().trim();
        stockManagementService.getItemByCode(itemCode).orElseThrow(() -> new ItemNotFoundException("Item '" + itemCode + "' not found."));

        System.out.print("Quantity Received: "); int quantity = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Expiry Date (YYYY-MM-DD, blank for none): "); String expiryDateStr = scanner.nextLine().trim();
        LocalDate expiryDate = expiryDateStr.isEmpty() ? null : LocalDate.parse(expiryDateStr, dateFormatter);
        System.out.print("Cost Per Unit (e.g., 5.25): "); BigDecimal costPerUnit = new BigDecimal(scanner.nextLine().trim());

        stockManagementService.receiveNewStockBatch(itemCode, quantity, expiryDate, costPerUnit);
        System.out.println("Stock batch received and added successfully.");
    }

    private void moveStockToShelf() throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        System.out.println("\n--- Move Stock to Shelf ---");
        System.out.print("Item Code: "); String itemCode = scanner.nextLine().trim();
        System.out.print("Quantity to Move to Shelf: "); int quantity = Integer.parseInt(scanner.nextLine().trim());

        stockManagementService.addToShelf(itemCode, quantity);
        System.out.println(quantity + " units of " + itemCode + " moved to shelf stock.");
    }

    private void moveStockToWebsite() throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        System.out.println("\n--- Move Stock to Website ---");
        System.out.print("Item Code: "); String itemCode = scanner.nextLine().trim();
        System.out.print("Quantity to Move to Website: "); int quantity = Integer.parseInt(scanner.nextLine().trim());

        stockManagementService.addToWebsiteStock(itemCode, quantity);
        System.out.println(quantity + " units of " + itemCode + " moved to website stock.");
    }

    private void viewAllItems() throws DatabaseOperationException {
        System.out.println("\n--- All Items ---");
        List<Item> items = stockManagementService.getAllItems();
        if (items.isEmpty()) { System.out.println("No items found."); return; }
        System.out.printf("%-10s | %-25s | %-15s | %-10s | %-13s | %-16s | %-10s%n",
                "Code", "Name", "Category", "Unit Price", "Reorder Level", "Reorder Quantity", "Description");
        System.out.println("-------------------------------------------------------------------------------------------------------------------------");
        for (Item item : items) {
            System.out.printf("%-10s | %-25s | %-15s | %-12.2f | %-13d | %-16d | %-10s%n",
                    item.getItemCode(), item.getName(), item.getCategory(), item.getUnitPrice(),
                    item.getReorderLevel(), item.getReorderQuantity(),
                    item.getDescription() != null && !item.getDescription().isEmpty() ? item.getDescription() : "N/A");
        }
    }

    private void viewStockDetailsByItemCode() throws DatabaseOperationException, ItemNotFoundException {
        System.out.println("\n--- Stock Details by Item Code ---");
        System.out.print("Item Code: "); String itemCode = scanner.nextLine().trim();
        stockManagementService.getItemByCode(itemCode).orElseThrow(() -> new ItemNotFoundException("Item '" + itemCode + "' not found."));

        System.out.println("Item: " + stockManagementService.getItemByCode(itemCode).get().getName() + " (" + itemCode + ")");

        System.out.println("\n-- Main Stock Batches --");
        List<StockBatch> stockBatches = stockManagementService.getStockBatchesByItemCode(itemCode);
        if (stockBatches.isEmpty()) System.out.println("  No main stock batches found.");
        else {
            System.out.printf("  %-7s | %-12s | %-10s | %-10s | %-10s | %-12s%n",
                    "BatchID", "Recv Date", "Qty Rcvd", "Current Qty", "Expiry Date", "Cost/Unit");
            System.out.println("  -----------------------------------------------------------------------");
            for (StockBatch batch : stockBatches) {
                System.out.printf("  %-7s | %-12s | %-10d | %-11d | %-11s | %-12.2f%n",
                        batch.getBatchId().toString(),
                        batch.getReceivedDate().format(dateFormatter),
                        batch.getReceivedQuantity(),
                        batch.getCurrentQuantityInStore(),
                        batch.getExpiryDate() != null ? batch.getExpiryDate().format(dateFormatter) : "N/A",
                        batch.getCostPerUnit());
            }
        }

        System.out.println("\n-- Shelf Stock --");
        // FIX START: Changed from List to Optional and handled accordingly
        Optional<ShelfStock> shelfStockOptional = stockManagementService.getShelfStockByItemCode(itemCode);
        int totalShelfStock = stockManagementService.getAvailableShelfStock(itemCode); // This already gives the total
        System.out.println("  Total on Shelf: " + totalShelfStock);
        if (shelfStockOptional.isEmpty()) {
            System.out.println("  No shelf stock found for this item.");
        } else {
            ShelfStock shelf = shelfStockOptional.get();
            System.out.printf("  %-13s | %-12s | %-12s%n", "ShelfStockID", "Qty on Shelf", "Last Updated");
            System.out.println("  ---------------------------------------------");
            System.out.printf("  %-13s | %-12d | %-12s%n",
                    shelf.getShelfStockId().toString(),
                    shelf.getQuantityOnShelf(),
                    shelf.getLastUpdatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        // FIX END

        System.out.println("\n-- Website Stock --");
        // FIX START: Changed from List to Optional and handled accordingly
        Optional<WebsiteStock> websiteStockOptional = stockManagementService.getWebsiteStockByItemCode(itemCode);
        int totalWebsiteStock = stockManagementService.getAvailableWebsiteStock(itemCode); // This already gives the total
        System.out.println("  Total on Website: " + totalWebsiteStock);
        if (websiteStockOptional.isEmpty()) {
            System.out.println("  No website stock found for this item.");
        } else {
            WebsiteStock website = websiteStockOptional.get();
            System.out.printf("  %-14s | %-10s | %-12s%n", "WebsiteStockID", "Qty Online", "Last Updated");
            System.out.println("  ---------------------------------------------");
            System.out.printf("  %-14s | %-10d | %-12s%n",
                    website.getWebsiteStockId().toString(),
                    website.getQuantityAvailableOnline(),
                    website.getLastUpdatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        // FIX END
    }

    private void viewAllStockBatches() throws DatabaseOperationException {
        System.out.println("\n--- All Stock Batches in Main Store ---");
        List<StockBatch> batches = stockManagementService.getAllStockBatches();
        if (batches.isEmpty()) { System.out.println("No stock batches found."); return; }
        System.out.printf("%-7s | %-10s | %-12s | %-10s | %-10s | %-11s | %-12s%n",
                "BatchID", "Item Code", "Recv Date", "Qty Rcvd", "Current Qty", "Expiry Date", "Cost/Unit");
        System.out.println("------------------------------------------------------------------------------------------");
        for (StockBatch batch : batches) {
            System.out.printf("%-7s | %-10s | %-12s | %-10d | %-11d | %-11s | %-12.2f%n",
                    batch.getBatchId().toString(),
                    batch.getItem().getItemCode(),
                    batch.getReceivedDate().format(dateFormatter),
                    batch.getReceivedQuantity(),
                    batch.getCurrentQuantityInStore(),
                    batch.getExpiryDate() != null ? batch.getExpiryDate().format(dateFormatter) : "N/A",
                    batch.getCostPerUnit());
        }
    }

    private void viewAllShelfStock() throws DatabaseOperationException {
        System.out.println("\n--- All Shelf Stock Entries ---");
        List<ShelfStock> shelfStocks = stockManagementService.getAllShelfStock();
        if (shelfStocks.isEmpty()) { System.out.println("No shelf stock entries found."); return; }
        System.out.printf("%-13s | %-10s | %-12s | %-19s%n",
                "ShelfStockID", "Item Code", "Qty on Shelf", "Last Updated Date");
        System.out.println("-----------------------------------------------------------------------");
        for (ShelfStock shelf : shelfStocks) {
            System.out.printf("%-13s | %-10s | %-12d | %-19s%n",
                    shelf.getShelfStockId().toString(),
                    shelf.getItem().getItemCode(),
                    shelf.getQuantityOnShelf(),
                    shelf.getLastUpdatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    private void viewAllWebsiteStock() throws DatabaseOperationException {
        System.out.println("\n--- All Website Stock Entries ---");
        List<WebsiteStock> websiteStocks = stockManagementService.getAllWebsiteStock();
        if (websiteStocks.isEmpty()) { System.out.println("No website stock entries found."); return; }
        System.out.printf("%-14s | %-10s | %-12s | %-19s%n",
                "WebsiteStockID", "Item Code", "Qty Online", "Last Updated Date");
        System.out.println("------------------------------------------------------------------------");
        for (WebsiteStock website : websiteStocks) {
            System.out.printf("%-14s | %-10s | %-12d | %-19s%n",
                    website.getWebsiteStockId().toString(),
                    website.getItem().getItemCode(),
                    website.getQuantityAvailableOnline(),
                    website.getLastUpdatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    private void processExpiredStock() throws DatabaseOperationException {
        System.out.println("\n--- Processing Expired Stock ---");
        System.out.print("Confirm removal of all expired stock batches (y/n): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Operation cancelled.");
            return;
        }

        List<StockBatch> removedBatches = stockManagementService.processAndRemoveExpiredStock();
        if (removedBatches.isEmpty()) {
            System.out.println("No expired stock batches found to remove.");
        } else {
            System.out.println("Removed " + removedBatches.size() + " expired stock batch(es):");
            for (StockBatch batch : removedBatches) {
                System.out.println("  - Batch ID: " + batch.getBatchId() + ", Item: " + batch.getItem().getItemCode() + ", Expiry Date: " + batch.getExpiryDate().format(dateFormatter));
            }
        }
    }
}