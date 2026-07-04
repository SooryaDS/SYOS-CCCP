package com.syos.web;

import com.syos.application.port.ReportingService;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.exception.DatabaseOperationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@WebServlet("/report")
public class ReportServlet extends HttpServlet {

    private ReportingService reportingService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public void init() throws ServletException {
        super.init();
        // ✅ Get the service from AppContext singleton
        this.reportingService = AppContext.getInstance().getReportingService();
        System.out.println("[ReportServlet] ReportingService initialized: " + (reportingService != null));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/report.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = req.getParameter("action");
        System.out.println("[ReportServlet] Action received: " + action);

        try {
            if (action == null) {
                req.setAttribute("error", "No action selected.");
            } else {
                switch (action) {
                    case "dailySales" -> handleDailySales(req);
                    case "reorder" -> handleReorder(req);
                    case "batchStock" -> handleBatchStock(req);
                    case "reshelving" -> handleReshelving(req);
                    case "billTransactions" -> handleBillTransactions(req);
                    default -> req.setAttribute("error", "Unknown action: " + action);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Unexpected error: " + e.getMessage());
        }

        req.getRequestDispatcher("/report.jsp").forward(req, resp);
    }

    private void handleDailySales(HttpServletRequest req) {
        try {
            String dateStr = req.getParameter("date");
            String typeStr = req.getParameter("transactionType");

            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            Optional<TransactionType> filter = parseTransactionType(typeStr);

            var report = reportingService.generateDailySalesReport(date, filter);

            if (report != null) {
                int itemCount = (report.getSaleItems() != null) ? report.getSaleItems().size() : 0;
                System.out.println("[ReportServlet] Daily Sales Report generated for " + date);
                System.out.println("[ReportServlet] Total Revenue: " + report.getOverallTotalRevenue());
                System.out.println("[ReportServlet] Total Items: " + itemCount);
            } else {
                System.out.println("[ReportServlet] Daily Sales Report is null for " + date);
            }

            req.setAttribute("dailySalesReport", report);

        } catch (DatabaseOperationException e) {
            req.setAttribute("error", "Error generating Daily Sales Report: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Unexpected error: " + e.getMessage());
        }
    }




    private void handleReorder(HttpServletRequest req) {
        try {
            var report = reportingService.generateReorderLevelReport();
            System.out.println("[ReportServlet] Reorder Report size: " + (report != null ? report.size() : 0));
            req.setAttribute("reorderReport", report);
        } catch (DatabaseOperationException e) {
            req.setAttribute("error", "Error generating Reorder Report: " + e.getMessage());
        }
    }

    private void handleBatchStock(HttpServletRequest req) {
        try {
            var report = reportingService.generateBatchStockReport();
            System.out.println("[ReportServlet] Batch Stock Report size: " + (report != null ? report.size() : 0));
            req.setAttribute("batchStockReport", report);
        } catch (DatabaseOperationException e) {
            req.setAttribute("error", "Error generating Batch Stock Report: " + e.getMessage());
        }
    }

    private void handleReshelving(HttpServletRequest req) {
        try {
            var report = reportingService.generateDailyReshelvingNeedsReport();
            System.out.println("[ReportServlet] Reshelving Report size: " + (report != null ? report.size() : 0));
            req.setAttribute("reshelvingReport", report);
        } catch (DatabaseOperationException e) {
            req.setAttribute("error", "Error generating Reshelving Report: " + e.getMessage());
        }
    }

    private void handleBillTransactions(HttpServletRequest req) {
        try {
            String start = req.getParameter("startDate");
            String end = req.getParameter("endDate");
            String typeStr = req.getParameter("transactionType");

            LocalDate startDate = (start == null || start.isEmpty()) ? null : LocalDate.parse(start, dateFormatter);
            LocalDate endDate = (end == null || end.isEmpty()) ? null : LocalDate.parse(end, dateFormatter);
            Optional<TransactionType> filter = parseTransactionType(typeStr);

            var report = reportingService.generateBillTransactionReport(startDate, endDate, filter);
            System.out.println("[ReportServlet] Bill Transaction Report size: " + (report != null ? report.size() : 0));
            req.setAttribute("billReport", report);

        } catch (DatabaseOperationException e) {
            req.setAttribute("error", "Error generating Bill Transaction Report: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Unexpected error: " + e.getMessage());
        }
    }

    private Optional<TransactionType> parseTransactionType(String typeStr) {
        if ("POS".equalsIgnoreCase(typeStr)) return Optional.of(TransactionType.POS_CASH);
        if ("ONLINE".equalsIgnoreCase(typeStr)) return Optional.of(TransactionType.ONLINE_SALE);
        return Optional.empty();
    }
}
