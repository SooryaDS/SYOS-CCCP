package com.syos;

import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.enums.UserRole;

import com.syos.application.port.*;
import com.syos.application.usecase.*;
import com.syos.domain.exception.DatabaseOperationException;

import com.syos.adapter.in.CLI.CounterSalesUI;
import com.syos.adapter.in.CLI.OnlineShopUI;
import com.syos.adapter.in.CLI.ReportUI;
import com.syos.adapter.in.CLI.StockUI;
import com.syos.adapter.in.CLI.UserUI;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.*;
import com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation.*;

import com.syos.adapter.out.util.DatabaseConnection;

import com.syos.service.payment.CashPaymentMethod;
import com.syos.service.payment.PaymentMethod;

import java.util.Scanner;
import java.util.function.Function;

public class MainApplication {

    private static AuthenticationService authenticationService;
    private static OnlineUserService onlineUserService;
    private static OnlineOrderingService onlineOrderingService;

    private static CounterSalesUI posView;
    private static StockUI stockView;
    private static ReportUI reportView;
    private static OnlineShopUI onlineShoppingView;
    private static UserUI userView;

    private static Scanner scanner;

    public static void main(String[] args) {
        try {
            initializeServices();
        } catch (DatabaseOperationException e) {
            System.err.println("CRITICAL ERROR: Failed to initialize application services due to a database issue.");
            e.printStackTrace();
            return;
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: An unexpected error occurred during application initialization.");
            e.printStackTrace();
            return;
        }

        System.out.println("========================================");
        System.out.println(" Welcome to the SYOS Application!");
        System.out.println("========================================");

        runPrimaryLoop();

        System.out.println("\nExiting SYOS Application. Goodbye!");
        if (scanner != null) {
            scanner.close();
        }
    }

    private static void initializeServices() throws DatabaseOperationException {
        DatabaseConnection dbConnection = new DatabaseConnection();

        // Instantiate Repositories with correct dependencies
        ItemRepository itemRepository = new ItemRepositoryImpl();
        BillRepository billRepository = new BillRepositoryImpl(itemRepository);
        StockBatchRepository stockBatchRepository = new StockBatchRepositoryImpl(itemRepository);
        ShelfStockRepository shelfStockRepository = new ShelfStockRepositoryImpl(itemRepository);
        WebsiteStockRepository websiteStockRepository = new WebsiteStockRepositoryImpl(itemRepository);

        OnlineUserRepository onlineUserRepository = new OnlineUserRepositoryImpl();
        EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();
        OnlineOrderRepository onlineOrderRepository = new OnlineOrderRepositoryImpl(itemRepository);

        // Instantiate Payment Method
        PaymentMethod onlinePaymentMethod = new CashPaymentMethod();

        // Instantiate Use Cases (Services)
        StockManagementService stockManagementService = new StockManagementServiceImplementation(
                itemRepository,
                stockBatchRepository,
                shelfStockRepository,
                websiteStockRepository
        );

        ReportingService reportingService = new ReportingServiceImplementation(billRepository, itemRepository, stockBatchRepository, shelfStockRepository);
        onlineUserService = new OnlineUserServiceImplementation(onlineUserRepository);
        BillingService billingService = new BillingServiceImplementation(itemRepository, billRepository);


        onlineOrderingService = new OnlineOrderingServiceImplementation(
                onlineOrderRepository,
                itemRepository,
                websiteStockRepository,
                billRepository,
                stockManagementService,
                onlinePaymentMethod,
                onlineUserRepository
        );

        authenticationService = new AuthenticationServiceImplementation(onlineUserRepository, employeeRepository);

        // Instantiate UI Adapters
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
            printLoginRegistrationMenu();

            System.out.print("Please enter your choice: ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("No input provided. Please enter a choice.");
                continue;
            }

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
            }
            catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static void printLoginRegistrationMenu() {
        System.out.println("\n--- SYOS ACCESS PORTAL ---");
        System.out.println("----------------------------------------");
        System.out.println("1. Login");
        System.out.println("2. Register as a New Customer");
        System.out.println("----------------------------------------");
        System.out.println("0. Exit Application");
        System.out.println("----------------------------------------");
    }

    private static void handleLogin() {
        printSystemLoginPrompt();
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

    private static void printSystemLoginPrompt() {
        System.out.println("\n--- SYSTEM LOGIN ---");
        System.out.println("------------------------------");
    }

    private static void runStaffMainMenu(AuthenticatedUser staffUser) {
        boolean loggedIn = true;
        while (loggedIn) {
            printStaffMenu(staffUser.getUsername());

            System.out.print("Enter choice: ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("No input provided. Please enter a choice.");
                continue;
            }

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
                        System.out.println("\nLogging out... Goodbye, " + staffUser.getUsername() + "!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
            catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (e.g., 1, 2, 3).");
            }
        }
    }

    private static void printStaffMenu(String username) {
        System.out.println("\n--- STAFF MAIN MENU ---");
        System.out.println("----------------------------------------");
        System.out.println("Welcome, " + username + "!");
        System.out.println("----------------------------------------");
        System.out.println("1. Point of Sale (POS)");
        System.out.println("2. Stock Management");
        System.out.println("3. Reporting");
        System.out.println("----------------------------------------");
        System.out.println("0. Logout");
        System.out.println("----------------------------------------");
    }


    private static void runCustomerMainMenu(AuthenticatedUser customerUser) {
        boolean loggedIn = true;
        onlineShoppingView.setCurrentUser(customerUser);

        while (loggedIn) {
            printCustomerMenu(customerUser.getUsername());

            System.out.print("Enter choice: ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("No input provided. Please enter a choice.");
                continue;
            }

            try {
                int choice = Integer.parseInt(line);
                switch (choice) {
                    case 1:
                        runSubMenu(onlineShoppingView::displayOnlineShoppingMenu, onlineShoppingView::handleOnlineShoppingAction);
                        break;
                    case 0:
                        loggedIn = false;
                        onlineShoppingView.setCurrentUser(null);
                        System.out.println("\nLogging out... Goodbye, " + customerUser.getUsername() + "!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
            catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (e.g., 1, 0).");
            }
        }
    }

    private static void printCustomerMenu(String username) {
        System.out.println("\n--- CUSTOMER MENU ---");
        System.out.println("----------------------------------------");
        System.out.println("Welcome, " + username + "!");
        System.out.println("----------------------------------------");
        System.out.println("1. Online Shopping Portal");
        System.out.println("----------------------------------------");
        System.out.println("0. Logout");
        System.out.println("----------------------------------------");
    }

    private static void runSubMenu(Runnable displayMenu, Function<Integer, Boolean> handleAction) {
        boolean inMenu = true;
        while(inMenu) {
            displayMenu.run();
            System.out.print("Enter choice: ");
            String choiceLine = scanner.nextLine().trim();
            if (choiceLine.isEmpty()) {
                System.out.println("No input provided. Please enter a number.");
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