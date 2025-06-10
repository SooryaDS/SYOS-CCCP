package com.syos;

import com.syos.adapter.in.CLI.*;


import com.syos.adapter.out.DatabaseSpecificAdaptors.implementation.*;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.*;

import com.syos.application.port.*;
import com.syos.application.usecase.*;
import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.enums.UserRole;
import com.syos.domain.exception.DatabaseOperationException;

import com.syos.service.payment.CashPaymentStrategy;
import com.syos.service.payment.PaymentStrategy;

import java.util.Scanner;
import java.util.function.Function;

public class App {

    private static AuthenticationService authenticationService;
    private static OnlineUserService onlineUserService;
    private static CounterSalesUI posView;
    private static StockUI stockView;
    private static ReportUI reportView;
    private static OnlineShopUI onlineShoppingView;
    private static UserUI userView;
    private static Scanner scanner;

    public static void main(String[] args) {
        try {
            Class.forName("com.syos.adapter.out.util.DatabaseConnection");
            initializeServices();
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL ERROR: Failed to load DatabaseConnection. Please ensure it's in the classpath.");
            e.printStackTrace();
            return;
        }

        System.out.println("========================================");
        System.out.println(" Welcome to the SYOS Application!");
        System.out.println("========================================");

        runPrimaryLoop();

        System.out.println("\nExiting SYOS Application. Goodbye!");
        scanner.close();
    }

    private static void initializeServices() {
        ItemRepository itemRepository = new ItemRepositoryImpl();
        BillRepository billRepository = new BillRepositoryImpl(itemRepository);
        StockBatchRepository stockBatchRepository = new StockBatchRepositoryImpl();
        ShelfStockRepository shelfStockRepository = new ShelfStockRepositoryImpl();
        WebsiteStockRepository websiteStockRepository = new WebsiteStockRepositoryImpl();
        OnlineUserRepository onlineUserRepository = new OnlineUserRepositoryImpl();
        EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();
        OnlineOrderRepository onlineOrderRepository = new OnlineOrderRepositoryImpl(itemRepository);

        StockManagementService stockManagementService = new StockManagementServiceImplementation(itemRepository, stockBatchRepository, shelfStockRepository, websiteStockRepository);
        ReportingService reportingService = new ReportingServiceImplementation(billRepository, itemRepository, stockBatchRepository, shelfStockRepository);
        onlineUserService = new OnlineUserServiceImplementation(onlineUserRepository);
        PaymentStrategy onlinePaymentStrategy = new CashPaymentStrategy();
        BillingService billingService = new BillingServiceImplementation(itemRepository, billRepository);
        OnlineOrderingService onlineOrderingService = new OnlineOrderingServiceImplementation(onlineOrderRepository, itemRepository, websiteStockRepository, billRepository, stockManagementService, onlinePaymentStrategy);
        authenticationService = new AuthenticationServiceImplementation(onlineUserRepository, employeeRepository);

        scanner = new Scanner(System.in);
        posView = new CounterSalesUI(billingService, stockManagementService, scanner);
        stockView = new StockUI(stockManagementService, scanner);
        reportView = new ReportUI(reportingService, scanner);
        userView = new UserUI(onlineUserService, scanner);
        onlineShoppingView = new OnlineShopUI(onlineOrderingService, onlineUserService, scanner);
    }

    private static void runPrimaryLoop() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- SYOS Login & Registration ---");
            System.out.println("1. Login");
            System.out.println("2. Register as a New Customer");
            System.out.println("0. Exit Application");
            System.out.print("Please enter your choice: ");

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                int choice = Integer.parseInt(line);
                switch (choice) {
                    case 1:
                        handleLogin();
                        break;
                    case 2:
                        userView.handleUserAction(1);
                        break;
                    case 0:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static void handleLogin() {
        System.out.println("\n--- System Login ---");
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        try {
            authenticationService.authenticate(username, password).ifPresentOrElse(
                    authenticatedUser -> {
                        System.out.println("\nLogin successful. Welcome, " + authenticatedUser.getUsername() + "!");
                        if (authenticatedUser.getRole() == UserRole.STAFF) {
                            runStaffMainMenu(authenticatedUser);
                        } else if (authenticatedUser.getRole() == UserRole.CUSTOMER) {
                            runCustomerMainMenu(authenticatedUser);
                        }
                    },
                    () -> System.out.println("Login failed. Invalid username or password.")
            );
        } catch (DatabaseOperationException e) {
            System.err.println("A database error occurred during login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runStaffMainMenu(AuthenticatedUser staffUser) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- Staff Main Menu (User: " + staffUser.getUsername() + ") ---");
            System.out.println("1. Point of Sale (POS)");
            System.out.println("2. Stock Management");
            System.out.println("3. Reporting");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                int choice = Integer.parseInt(line);
                switch (choice) {
                    case 1:
                        runSubMenu(posView::displayPosMenu, posView::handlePosAction);
                        break;
                    case 2:
                        runSubMenu(stockView::displayStockMenu, stockView::handleStockAction);
                        break;
                    case 3:
                        runSubMenu(reportView::displayReportMenu, reportView::handleReportAction);
                        break;
                    case 0:
                        loggedIn = false;
                        System.out.println("Logging out...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static void runCustomerMainMenu(AuthenticatedUser customerUser) {
        boolean loggedIn = true;
        onlineShoppingView.setCurrentUser(customerUser);

        while (loggedIn) {
            System.out.println("\n--- Customer Menu (User: " + customerUser.getUsername() + ") ---");
            System.out.println("1. Online Shopping Portal");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                int choice = Integer.parseInt(line);
                switch (choice) {
                    case 1:
                        runSubMenu(onlineShoppingView::displayOnlineShoppingMenu, onlineShoppingView::handleOnlineShoppingAction);
                        break;
                    case 0:
                        loggedIn = false;
                        onlineShoppingView.setCurrentUser(null);
                        System.out.println("Logging out...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static void runSubMenu(Runnable displayMenu, Function<Integer, Boolean> handleAction) {
        boolean inMenu = true;
        while(inMenu) {
            displayMenu.run();
            System.out.print("Enter choice: ");
            String choiceLine = scanner.nextLine().trim();
            if (choiceLine.isEmpty()) {
                System.out.println("No input provided.");
                continue;
            }
            try {
                int choice = Integer.parseInt(choiceLine);
                inMenu = handleAction.apply(choice);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}