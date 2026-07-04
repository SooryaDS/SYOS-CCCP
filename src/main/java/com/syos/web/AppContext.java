package com.syos.web;

import com.syos.application.port.BillingService;
import com.syos.application.port.OnlineUserService;
import com.syos.application.port.StockManagementService;
import com.syos.application.port.AuthenticationService;
import com.syos.application.port.OnlineOrderingService;
import com.syos.application.port.ReportingService;

import com.syos.application.usecase.BillingServiceImplementation;
import com.syos.application.usecase.StockManagementServiceImplementation;
import com.syos.application.usecase.OnlineUserServiceImplementation;
import com.syos.application.usecase.AuthenticationServiceImplementation;
import com.syos.application.usecase.OnlineOrderingServiceImplementation;
import com.syos.application.usecase.ReportingServiceImplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.*;
import com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation.*;

import com.syos.service.payment.CashPaymentMethod;
import com.syos.service.payment.PaymentMethod;

public class AppContext {

    private static AppContext instance;

    // Repositories
    private final ItemRepository itemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ShelfStockRepository shelfStockRepository;
    private final WebsiteStockRepository websiteStockRepository;
    private final BillRepository billRepository;
    private final OnlineUserRepository onlineUserRepository;
    private final EmployeeRepository employeeRepository;

    // Services
    private final StockManagementService stockManagementService;
    private final BillingService billingService;
    private final OnlineUserService onlineUserService;
    private final AuthenticationService authenticationService;
    private final OnlineOrderingService onlineOrderingService;
    private final ReportingService reportingService;

    private AppContext() {
        // Initialize repositories
        this.itemRepository = new ItemRepositoryImpl();
        this.stockBatchRepository = new StockBatchRepositoryImpl(itemRepository);
        this.shelfStockRepository = new ShelfStockRepositoryImpl(itemRepository);
        this.websiteStockRepository = new WebsiteStockRepositoryImpl(itemRepository);
        this.billRepository = new BillRepositoryImpl(itemRepository);
        this.onlineUserRepository = new OnlineUserRepositoryImpl();
        this.employeeRepository = new EmployeeRepositoryImpl();

        // Initialize services
        this.stockManagementService = new StockManagementServiceImplementation(
                itemRepository,
                stockBatchRepository,
                shelfStockRepository,
                websiteStockRepository
        );

        this.billingService = new BillingServiceImplementation(
                itemRepository,
                billRepository
        );

        this.onlineUserService = new OnlineUserServiceImplementation(onlineUserRepository);
        this.authenticationService = new AuthenticationServiceImplementation(onlineUserRepository, employeeRepository);

        // Payment Method
        PaymentMethod paymentMethod = new CashPaymentMethod();

        // Online Ordering Service
        this.onlineOrderingService = new OnlineOrderingServiceImplementation(
                new OnlineOrderRepositoryImpl(itemRepository),  // OnlineOrderRepository
                itemRepository,                   // ItemRepository
                websiteStockRepository,           // WebsiteStockRepository
                billRepository,                   // BillRepository
                stockManagementService,           // StockManagementService
                paymentMethod,                    // PaymentMethod
                onlineUserRepository              // OnlineUserRepository
        );

        // Reporting Service
        this.reportingService = new ReportingServiceImplementation(
                billRepository,
                itemRepository,
                stockBatchRepository,
                shelfStockRepository
        );
    }

    // Singleton accessor
    public static AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    // Getters for services
    public StockManagementService getStockManagementService() {
        return stockManagementService;
    }

    public BillingService getBillingService() {
        return billingService;
    }

    public OnlineUserService getOnlineUserService() {
        return onlineUserService;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public OnlineOrderingService getOnlineOrderingService() {
        return onlineOrderingService;
    }

    public ReportingService getReportingService() {
        return reportingService;
    }
}
