package com.syos.adapter.in.CLI;

import com.syos.domain.model.Bill;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.InsufficientStockException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.PaymentException;
import com.syos.application.port.BillingService;
import com.syos.application.port.StockManagementService;
import com.syos.service.payment.CashPaymentStrategy;
import com.syos.service.payment.PaymentStrategy;

import java.math.BigDecimal; // Import BigDecimal
import java.util.Scanner;

public class CounterSalesUI {
    private final BillingService billingService;
    private final StockManagementService stockManagementService;
    private final Scanner scanner;
    private Bill currentBill;

    public CounterSalesUI(BillingService billingService, StockManagementService stockManagementService, Scanner scanner) {
        this.billingService = billingService;
        this.stockManagementService = stockManagementService;
        this.scanner = scanner;
    }

    public void displayPosMenu() {
        System.out.println("\n--- Point of Sale Menu ---");
        if (currentBill == null) System.out.println("1. Start New Bill");
        else {
            System.out.println("Current Bill #: " + currentBill.getBillSerialNumber() + " | Date: " + currentBill.getBillDate().toLocalDate());
            System.out.println("1. Add Item to Current Bill");
            System.out.println("2. View Current Bill");
            System.out.println("3. Finalize Current Bill");
            System.out.println("4. Cancel Current Bill");
        }
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
    }

    public boolean handlePosAction(int choice) {
        try {
            switch (choice) {
                case 1:
                    if (currentBill == null) startNewBill(); else handleAddItemToBill();
                    break;
                case 2:
                    if (currentBill != null) System.out.println(currentBill.generateReceipt()); else System.out.println("No active bill to view.");
                    break;
                case 3:
                    if (currentBill != null) handleFinalizeBill(); else System.out.println("No active bill to finalize.");
                    break;
                case 4:
                    if (currentBill != null) { System.out.println("Bill #" + currentBill.getBillSerialNumber() + " cancelled."); currentBill = null; } else System.out.println("No active bill to cancel.");
                    break;
                case 0:
                    if (currentBill != null) {
                        System.out.println("Warning: Current bill #" + currentBill.getBillSerialNumber() + " is active and will be lost. Proceed? (y/n)");
                        if (!"y".equals(scanner.nextLine().trim().toLowerCase())) return true; // Stay in POS menu
                        currentBill = null;
                    }
                    return false; // Exit to main menu
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Please enter a valid number.");
        } catch (PaymentException | DatabaseOperationException | ItemNotFoundException | InsufficientStockException | IllegalStateException | IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
        return true;
    }

    private void startNewBill() throws DatabaseOperationException {
        currentBill = billingService.startNewBill(TransactionType.POS_CASH);
        System.out.println("New bill #" + currentBill.getBillSerialNumber() + " started for today.");
    }

    private void handleAddItemToBill() throws ItemNotFoundException, InsufficientStockException, DatabaseOperationException {
        if (currentBill == null) { System.out.println("Please start a new bill first."); return; }

        System.out.print("Enter item code: ");
        String itemCode = scanner.nextLine().trim();
        if (itemCode.isEmpty()) { System.out.println("Item code cannot be empty."); return; }

        System.out.print("Enter quantity: ");
        int quantity = Integer.parseInt(scanner.nextLine().trim());

        billingService.addItemToBill(currentBill, itemCode, quantity);
        System.out.println(currentBill.generateReceipt());
    }

    private void handleFinalizeBill() throws DatabaseOperationException, IllegalStateException, InsufficientStockException, ItemNotFoundException, PaymentException {
        if (currentBill == null || currentBill.getBillItems().isEmpty()) {
            System.out.println("No items in the bill to finalize.");
            return;
        }

        System.out.println("\n--- Finalizing Bill ---");

        // Get cash tendered from user here in the UI layer
        BigDecimal amountTendered;
        while (true) {
            System.out.print("Enter cash tendered: ");
            String cashInput = scanner.nextLine().trim();
            try {
                amountTendered = new BigDecimal(cashInput);
                if (amountTendered.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("Amount tendered cannot be negative. Please enter a valid amount.");
                } else {
                    break; // Valid amount entered
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a numeric value.");
            }
        }

        // 1. Create the specific payment strategy (now stateless, no constructor args)
        PaymentStrategy cashPayment = new CashPaymentStrategy();

        // 2. Pass the bill, the chosen strategy, stock service, and the amount tendered to the service.
        // The BillingService.finalizeBill method now expects four arguments.
        billingService.finalizeBill(currentBill, cashPayment, stockManagementService, amountTendered);

        System.out.println("\n--- Final Receipt ---");
        System.out.println(currentBill.generateReceipt());
        System.out.println("--- Bill Finalized Successfully! ---");
        currentBill = null; // Reset for a new bill
    }
}
