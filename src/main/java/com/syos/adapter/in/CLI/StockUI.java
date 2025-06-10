package com.syos.adapter.in.CLI;

import com.syos.domain.model.StockBatch;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.application.port.StockManagementService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class StockUI {
    private final StockManagementService stockManagementService;
    private final Scanner scanner;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public StockUI(StockManagementService stockManagementService, Scanner scanner) {
        this.stockManagementService = stockManagementService;
        this.scanner = scanner;
    }

    public void displayStockMenu() {
        System.out.println("\n--- Stock Management Menu ---");
        System.out.println("1. Add New Stock Batch");
        System.out.println("2. View Stock Batches for an Item (Main Store)");
        System.out.println("3. Move Stock from Store to Shelf");
        System.out.println("4. Move Stock from Store to Website Inventory");
        System.out.println("5. Process and Remove Expired Stock"); // New Option
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
    }

    public boolean handleStockAction(int choice) {
        switch (choice) {
            case 1:
                handleAddStockBatch();
                break;
            case 2:
                handleViewStockBatchesForItem();
                break;
            case 3:
                handleMoveStockToShelf();
                break;
            case 4:
                handleMoveStockToWebsiteInventory();
                break;
            case 5:
                handleProcessExpiredStock(); // New Handler
                break;
            case 0:
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
        return true;
    }

    private void handleAddStockBatch() {
        try {
            System.out.println("\n--- Add New Stock Batch ---");
            System.out.print("Enter Item Code: "); String itemCode = scanner.nextLine().trim();
            System.out.print("Enter Purchase Date (YYYY-MM-DD): "); LocalDate purchaseDate = LocalDate.parse(scanner.nextLine().trim(), dateFormatter);
            System.out.print("Enter Quantity Received: "); int quantityReceived = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter Expiry Date (YYYY-MM-DD, leave blank if none): "); String expiryDateStr = scanner.nextLine().trim();
            LocalDate expiryDate = expiryDateStr.isEmpty() ? null : LocalDate.parse(expiryDateStr, dateFormatter);
            StockBatch addedBatch = stockManagementService.addNewStockBatch(itemCode, purchaseDate, quantityReceived, expiryDate);
            System.out.println("Stock batch added successfully!\nDetails: " + addedBatch);
        } catch (DateTimeParseException e) { System.out.println("Error: Invalid date format. Please use yyyy-MM-dd. " + e.getMessage());
        } catch (NumberFormatException e) { System.out.println("Error: Invalid quantity.");
        } catch (ItemNotFoundException | DatabaseOperationException | IllegalArgumentException e) { System.out.println("Error: " + e.getMessage());
        } catch (Exception e) { System.out.println("An unexpected error occurred: " + e.getMessage()); e.printStackTrace(); }
    }

    private void handleViewStockBatchesForItem() {
        try {
            System.out.println("\n--- View Stock Batches for Item (Main Store) ---");
            System.out.print("Enter Item Code: "); String itemCode = scanner.nextLine().trim();
            List<StockBatch> batches = stockManagementService.getStockBatchesForItem(itemCode);
            if (batches.isEmpty()) System.out.println("No stock batches found in the main store for item code: " + itemCode);
            else {
                System.out.println("Stock Batches for " + itemCode + " (Main Store):");
                System.out.printf("%-10s | %-15s | %-12s | %-10s | %-10s | %-12s%n", "Batch ID", "Item Code", "Purch. Date", "Qty Rcvd", "Qty Store", "Expiry Date");
                System.out.println("---------------------------------------------------------------------------------------");
                for (StockBatch batch : batches) {
                    System.out.printf("%-10d | %-15s | %-12s | %-10d | %-10d | %-12s%n", batch.getBatchId(), batch.getItemCode(),
                            batch.getPurchaseDate().format(dateFormatter), batch.getQuantityReceived(),
                            batch.getCurrentQuantityInStore(), batch.getExpiryDate() != null ? batch.getExpiryDate().format(dateFormatter) : "N/A");
                }
            }
        } catch (ItemNotFoundException | DatabaseOperationException | IllegalArgumentException e) { System.out.println("Error: " + e.getMessage());
        } catch (Exception e) { System.out.println("An unexpected error occurred: " + e.getMessage()); e.printStackTrace(); }
    }

    private void handleMoveStockToShelf() {
        try {
            System.out.println("\n--- Move Stock from Store to Shelf ---");
            System.out.print("Enter Item Code to move: "); String itemCode = scanner.nextLine().trim();
            System.out.print("Enter Quantity to move to shelf: "); int quantityToMove = Integer.parseInt(scanner.nextLine().trim());
            String resultMessage = stockManagementService.moveStockToShelf(itemCode, quantityToMove);
            System.out.println(resultMessage);
        } catch (NumberFormatException e) { System.out.println("Error: Invalid quantity.");
        } catch (ItemNotFoundException | InsufficientStockException | DatabaseOperationException | IllegalArgumentException e) { System.out.println("Error moving stock: " + e.getMessage());
        } catch (Exception e) { System.out.println("An unexpected error occurred while moving stock: " + e.getMessage()); e.printStackTrace(); }
    }

    private void handleMoveStockToWebsiteInventory() {
        try {
            System.out.println("\n--- Move Stock from Store to Website Inventory ---");
            System.out.print("Enter Item Code to move: ");
            String itemCode = scanner.nextLine().trim();
            System.out.print("Enter Quantity to move to website inventory: ");
            int quantityToMove = Integer.parseInt(scanner.nextLine().trim());
            String resultMessage = stockManagementService.moveStockToWebsiteInventory(itemCode, quantityToMove);
            System.out.println(resultMessage);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid quantity. Please enter a number.");
        } catch (ItemNotFoundException | InsufficientStockException | DatabaseOperationException | IllegalArgumentException e) {
            System.out.println("Error moving stock to website: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred while moving stock to website: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleProcessExpiredStock() {
        try {
            System.out.println("\n--- Processing Expired Stock ---");
            System.out.print("Are you sure you want to find and remove all expired stock batches from the main store? (y/n): ");
            String confirmation = scanner.nextLine().trim();
            if (!"y".equalsIgnoreCase(confirmation)) {
                System.out.println("Operation cancelled.");
                return;
            }

            List<StockBatch> removedBatches = stockManagementService.processAndRemoveExpiredStock();

            if (removedBatches.isEmpty()) {
                System.out.println("No expired stock batches found to remove.");
            } else {
                System.out.println("Successfully removed the following " + removedBatches.size() + " expired stock batch(es):");
                for (StockBatch batch : removedBatches) {
                    System.out.println(" - " + batch);
                }
            }
        } catch (DatabaseOperationException e) {
            System.out.println("Database Error processing expired stock: " + e.getMessage());
            System.out.println("This might be due to expired stock being present on shelves or website. Please check and remove manually before processing.");
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}