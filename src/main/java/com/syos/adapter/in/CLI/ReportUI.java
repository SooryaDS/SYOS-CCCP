package com.syos.adapter.in.CLI;

import com.syos.adapter.dto.*;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.application.port.ReportingService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ReportUI {
    private final ReportingService reportingService;
    private final Scanner scanner;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public ReportUI(ReportingService reportingService, Scanner scanner) {
        this.reportingService = reportingService;
        this.scanner = scanner;
    }

    public void displayReportMenu() {
        System.out.println("\n--- Reporting Menu ---");
        System.out.println("1. Daily Sales Report");
        System.out.println("2. Reorder Level Report");
        System.out.println("3. Batch-wise Stock Report");
        System.out.println("4. Daily Reshelving Needs Report");
        System.out.println("5. Bill Transaction Report"); // New option
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
    }

    public boolean handleReportAction(int choice) {
        switch (choice) {
            case 1:
                handleDailySalesReport();
                break;
            case 2:
                handleReorderLevelReport();
                break;
            case 3:
                handleBatchStockReport();
                break;
            case 4:
                handleDailyReshelvingNeedsReport();
                break;
            case 5:
                handleBillTransactionReport(); // New handler
                break;
            case 0:
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
        return true;
    }

    private void handleDailySalesReport() {
        try {
            System.out.println("\n--- Daily Sales Report ---");
            System.out.print("Enter Report Date (YYYY-MM-DD): ");
            LocalDate reportDate = LocalDate.parse(scanner.nextLine().trim(), dateFormatter);

            System.out.print("Filter by Transaction Type? (1. POS, 2. Online, 3. Combined - default): ");
            String typeChoiceStr = scanner.nextLine().trim();
            Optional<TransactionType> transactionTypeFilter = Optional.empty();
            if ("1".equals(typeChoiceStr)) {
                transactionTypeFilter = Optional.of(TransactionType.POS_CASH);
            } else if ("2".equals(typeChoiceStr)) {
                // FIX: Changed ONLINE_CASH to ONLINE_SALE
                transactionTypeFilter = Optional.of(TransactionType.ONLINE_SALE);
            } else if (!"3".equals(typeChoiceStr) && !typeChoiceStr.isEmpty()) {
                System.out.println("Invalid type choice, defaulting to Combined.");
            }

            DailySalesReportDTO reportDTO = reportingService.generateDailySalesReport(reportDate, transactionTypeFilter);

            System.out.println("\n--- Sales Report for " + reportDate.format(dateFormatter) +
                    (transactionTypeFilter.map(t -> " (" + t.name() + ")").orElse(" (Combined)")) + " ---");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-10s | %-25s | %10s | %15s%n", "Item Code", "Item Name", "Qty Sold", "Total Revenue");
            System.out.println("--------------------------------------------------------------------------");

            if (reportDTO.getSaleItems().isEmpty()) {
                System.out.println("No sales recorded for this period/filter.");
            } else {
                for (DailySalesReportItemDTO itemDTO : reportDTO.getSaleItems()) {
                    System.out.println(itemDTO.toString());
                }
            }
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-48s | %15.2f%n", "OVERALL TOTAL REVENUE:", reportDTO.getOverallTotalRevenue());
            System.out.println("--------------------------------------------------------------------------");

        } catch (DateTimeParseException e) {
            System.out.println("Error: Invalid date format. Please use YYYY-MM-DD. " + e.getMessage());
        } catch (DatabaseOperationException | IllegalArgumentException e) {
            System.out.println("Error generating report: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleReorderLevelReport() {
        try {
            System.out.println("\n--- Reorder Level Report ---");
            List<ReorderReportItemDTO> reportItems = reportingService.generateReorderLevelReport();

            if (reportItems.isEmpty()) {
                System.out.println("All items are currently above their reorder level thresholds.");
            } else {
                System.out.println("Items needing reorder (Current Stock < Threshold):");
                System.out.println("--------------------------------------------------------------------");
                System.out.printf("%-10s | %-25s | %10s | %10s%n", "Item Code", "Item Name", "Curr.Stock", "Threshold");
                System.out.println("--------------------------------------------------------------------");
                for (ReorderReportItemDTO item : reportItems) {
                    System.out.println(item.toString());
                }
                System.out.println("--------------------------------------------------------------------");
            }
        } catch (DatabaseOperationException e) {
            System.out.println("Database Error generating reorder report: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleBatchStockReport() {
        try {
            System.out.println("\n--- Batch-wise Stock Report (Main Store) ---");
            List<BatchStockReportItemDTO> reportItems = reportingService.generateBatchStockReport();

            if (reportItems.isEmpty()) {
                System.out.println("No stock batches found in the main store.");
            } else {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.printf("%-10s | %-20s | %-8s | %-12s | %-10s | %-10s | %-12s%n",
                        "Item Code", "Item Name", "Batch ID", "Purch. Date", "Qty Rcvd", "Qty Store", "Expiry Date");
                System.out.println("----------------------------------------------------------------------------------------------------");
                for (BatchStockReportItemDTO item : reportItems) {
                    System.out.println(item.toString());
                }
                System.out.println("----------------------------------------------------------------------------------------------------");
            }
        } catch (DatabaseOperationException e) {
            System.out.println("Database Error generating batch stock report: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDailyReshelvingNeedsReport() {
        try {
            System.out.println("\n--- Daily Reshelving Needs Report ---");
            List<ReshelvingNeedsItemDTO> reportItems = reportingService.generateDailyReshelvingNeedsReport();

            if (reportItems.isEmpty()) {
                System.out.println("No items currently need reshelving (all shelf stock >= reorder threshold).");
            } else {
                System.out.println("Items needing reshelving (Shelf Qty < Reorder Threshold):");
                System.out.println("---------------------------------------------------------------------------------");
                System.out.printf("%-10s | %-25s | %10s | %10s | %10s%n",
                        "Item Code", "Item Name", "Shelf Qty", "Threshold", "Needs Qty");
                System.out.println("---------------------------------------------------------------------------------");
                for (ReshelvingNeedsItemDTO item : reportItems) {
                    System.out.println(item.toString());
                }
                System.out.println("---------------------------------------------------------------------------------");
            }
        } catch (DatabaseOperationException e) {
            System.out.println("Database Error generating reshelving needs report: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleBillTransactionReport() {
        try {
            System.out.println("\n--- Bill Transaction Report ---");
            System.out.print("Enter Start Date (YYYY-MM-DD, leave blank for no start date): ");
            String startDateStr = scanner.nextLine().trim();
            LocalDate startDate = startDateStr.isEmpty() ? null : LocalDate.parse(startDateStr, dateFormatter);

            System.out.print("Enter End Date (YYYY-MM-DD, leave blank for no end date): ");
            String endDateStr = scanner.nextLine().trim();
            LocalDate endDate = endDateStr.isEmpty() ? null : LocalDate.parse(endDateStr, dateFormatter);

            System.out.print("Filter by Transaction Type? (1. POS, 2. Online, 3. All - default): ");
            String typeChoiceStr = scanner.nextLine().trim();
            Optional<TransactionType> transactionTypeFilter = Optional.empty();
            if ("1".equals(typeChoiceStr)) {
                transactionTypeFilter = Optional.of(TransactionType.POS_CASH);
            } else if ("2".equals(typeChoiceStr)) {
                // FIX: Changed ONLINE_CASH to ONLINE_SALE
                transactionTypeFilter = Optional.of(TransactionType.ONLINE_SALE);
            } else if (!"3".equals(typeChoiceStr) && !typeChoiceStr.isEmpty()) {
                System.out.println("Invalid type choice, defaulting to All.");
            }

            List<BillSummaryDTO> bills = reportingService.generateBillTransactionReport(startDate, endDate, transactionTypeFilter);

            System.out.println("\n--- Bill Transactions ---");
            System.out.println("Filters: " +
                    (startDate != null ? "From " + startDate.format(dateFormatter) : "Any Date") + " | " +
                    (endDate != null ? "To " + endDate.format(dateFormatter) : "Any Date") + " | " +
                    (transactionTypeFilter.map(Enum::name).orElse("All Types")));
            System.out.println("-----------------------------------------------------------------------------------");
            System.out.printf("%-10s | %-20s | %-12s | %-15s | %10s%n",
                    "Bill No.", "Date", "Type", "Customer ID", "Total Amt.");
            System.out.println("-----------------------------------------------------------------------------------");

            if (bills.isEmpty()) {
                System.out.println("No bill transactions found for the selected criteria.");
            } else {
                for (BillSummaryDTO billSummary : bills) {
                    System.out.println(billSummary.toString());
                }
            }
            System.out.println("-----------------------------------------------------------------------------------");

        } catch (DateTimeParseException e) {
            System.out.println("Error: Invalid date format. Please use YYYY-MM-DD. " + e.getMessage());
        } catch (DatabaseOperationException e) {
            System.out.println("Database Error generating bill transaction report: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}