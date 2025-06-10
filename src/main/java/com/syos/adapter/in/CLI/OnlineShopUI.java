// File: src/main/java/com/syos/ui/OnlineShopUI.java
package com.syos.adapter.in.CLI;

import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.model.Bill;
import com.syos.domain.model.OnlineOrder;

import com.syos.application.port.OnlineOrderingService;
import com.syos.application.port.OnlineUserService;
import java.util.Scanner;

public class OnlineShopUI {
    private final OnlineOrderingService onlineOrderingService;
    private final OnlineUserService onlineUserService;
    private final Scanner scanner;
    private AuthenticatedUser currentUser;
    private OnlineOrder currentOnlineOrder;

    public OnlineShopUI(OnlineOrderingService onlineOrderingService, OnlineUserService onlineUserService, Scanner scanner) {
        this.onlineOrderingService = onlineOrderingService;
        this.onlineUserService = onlineUserService;
        this.scanner = scanner;
    }

    /**
     * Sets the currently logged-in user for this view session.
     * It also fetches or creates an active order for that user.
     * @param user The authenticated user.
     */
    public void setCurrentUser(AuthenticatedUser user) {
        this.currentUser = user;
        if (user != null) {
            try {
                this.currentOnlineOrder = onlineOrderingService.getActiveOrderForUser(user.getId());
            } catch (DatabaseOperationException e) {
                System.err.println("Error fetching active order for user " + user.getUsername() + ": " + e.getMessage());
                this.currentOnlineOrder = null;
            }
        } else {
            this.currentOnlineOrder = null;
        }
    }

    public void displayOnlineShoppingMenu() {
        System.out.println("\n--- Online Shopping Portal (User: " + currentUser.getUsername() + ") ---");
        if (currentOnlineOrder != null) {
            System.out.println("Current Order ID: " + currentOnlineOrder.getOrderId() + " | Total: " + String.format("%.2f", currentOnlineOrder.getCalculatedTotalAmount()));
        } else {
            System.out.println("No active order. One will be created when you add an item.");
        }
        System.out.println("1. Add Item to Order");
        System.out.println("2. View Current Order");
        System.out.println("3. Update Item Quantity in Order");
        System.out.println("4. Remove Item from Order");
        System.out.println("5. Checkout Order");
        System.out.println("0. Back to Main Menu");
    }

    public boolean handleOnlineShoppingAction(int choice) {
        switch (choice) {
            case 1: handleAddItemToOrder(); break;
            case 2: handleViewCurrentOrder(); break;
            case 3: handleUpdateOrderItemQuantity(); break;
            case 4: handleRemoveItemFromOrder(); break;
            case 5: handleCheckoutOrder(); break;
            case 0: return false; // Signal to exit this sub-menu
            default: System.out.println("Invalid choice. Please try again.");
        }
        return true; // Signal to stay in this sub-menu
    }

    private void handleAddItemToOrder() {
        try {
            System.out.print("Enter Item Code to add: ");
            String itemCode = scanner.nextLine().trim();
            System.out.print("Enter Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());
            currentOnlineOrder = onlineOrderingService.addItemToActiveOrder(currentUser.getId(), itemCode, quantity);
            System.out.println("Item added successfully.");
        } catch (NumberFormatException e) {
            System.err.println("Invalid quantity. Please enter a number.");
        } catch (Exception e) {
            System.err.println("Error adding item: " + e.getMessage());
        }
    }

    private void handleViewCurrentOrder() {
        if (currentOnlineOrder == null || currentOnlineOrder.getItems().isEmpty()) {
            System.out.println("Your online order is currently empty.");
            return;
        }
        System.out.println("\n--- Current Online Order ---");
        System.out.println("Order ID: " + currentOnlineOrder.getOrderId());
        System.out.println("Status: " + currentOnlineOrder.getStatus());
        System.out.println("Items:");
        currentOnlineOrder.getItems().forEach(item -> System.out.println("  " + item));
        System.out.println("-----------------------------------");
        System.out.println("Total: " + String.format("%.2f", currentOnlineOrder.getCalculatedTotalAmount()));

    }

    private void handleUpdateOrderItemQuantity() {
        if (currentOnlineOrder == null || currentOnlineOrder.getItems().isEmpty()) {
            System.out.println("Your order is empty. Add items first.");
            return;
        }
        try {
            System.out.print("Enter Item Code to update: ");
            String itemCode = scanner.nextLine().trim();
            System.out.print("Enter new Quantity (0 to remove): ");
            int newQuantity = Integer.parseInt(scanner.nextLine().trim());
            currentOnlineOrder = onlineOrderingService.updateOrderItemQuantity(currentUser.getId(), itemCode, newQuantity);
            System.out.println("Order updated successfully.");
        } catch (NumberFormatException e) {
            System.err.println("Invalid quantity. Please enter a number.");
        } catch (Exception e) {
            System.err.println("Error updating quantity: " + e.getMessage());
        }
    }

    private void handleRemoveItemFromOrder() {
        if (currentOnlineOrder == null || currentOnlineOrder.getItems().isEmpty()) {
            System.out.println("Your order is empty.");
            return;
        }
        try {
            System.out.print("Enter Item Code to remove: ");
            String itemCode = scanner.nextLine().trim();
            currentOnlineOrder = onlineOrderingService.removeItemFromActiveOrder(currentUser.getId(), itemCode);
            System.out.println("Item removed successfully.");
        } catch (Exception e) {
            System.err.println("Error removing item: " + e.getMessage());
        }
    }

    private void handleCheckoutOrder() {
        if (currentOnlineOrder == null || currentOnlineOrder.getItems().isEmpty()) {
            System.out.println("Your order is empty. Add items before checking out.");
            return;
        }
        try {
            System.out.println("\n--- Checkout ---");
            System.out.print("Enter Shipping Address: ");
            String shippingAddress = scanner.nextLine().trim();
            if (shippingAddress.isEmpty()) {
                System.out.println("Shipping address cannot be empty. Checkout cancelled.");
                return;
            }

            System.out.print("Confirm checkout? (y/n): ");
            if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
                Bill finalizedBill = onlineOrderingService.checkoutOrder(currentUser.getId(), shippingAddress);
                System.out.println("\nCheckout successful! Your order has been placed.");
                System.out.println(finalizedBill.generateReceipt());
                // Get a new, empty order for the user for their next session
                currentOnlineOrder = onlineOrderingService.getActiveOrderForUser(currentUser.getId());
            } else {
                System.out.println("Checkout cancelled.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred during checkout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}