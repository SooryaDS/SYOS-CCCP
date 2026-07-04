package com.syos.web;

import com.syos.application.port.StockManagementService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Get the singleton app context
        AppContext appContext = AppContext.getInstance();

        // Store the StockManagementService in ServletContext so servlets can access it
        StockManagementService stockService = appContext.getStockManagementService();
        sce.getServletContext().setAttribute("stockService", stockService);

        System.out.println("[AppInitializer] StockManagementService registered ");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Optional clean-up
        System.out.println("[AppInitializer] Application shutting down...");
    }
}
