package com.syos.web;

import com.syos.application.port.BillingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.model.Bill;
import com.syos.service.payment.CashPaymentMethod;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/counter")
public class CounterSalesServlet extends HttpServlet {

    private final BillingService billingService;
    private final StockManagementService stockService;

    public CounterSalesServlet() {
        var ctx = AppContext.getInstance();
        this.billingService = ctx.getBillingService();
        this.stockService = ctx.getStockManagementService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.getRequestDispatcher("/counterSales.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession session = req.getSession(true);
        Bill currentBill = (Bill) session.getAttribute("currentBill");
        String action = req.getParameter("action");

        try {
            switch (action) {
                case "Start New Bill" -> {
                    currentBill = billingService.startNewBill(TransactionType.POS_CASH);
                    session.setAttribute("currentBill", currentBill);
                }
                case "Add Item" -> {
                    if (currentBill == null) {
                        req.setAttribute("error", "Please start a new bill first.");
                    } else {
                        String itemCode = req.getParameter("itemCode");
                        int qty = Integer.parseInt(req.getParameter("quantity"));
                        billingService.addItemToBill(currentBill, itemCode, qty);
                        session.setAttribute("currentBill", currentBill);
                    }
                }
                case "Finalize Bill" -> {
                    if (currentBill == null) {
                        req.setAttribute("error", "No bill started.");
                    } else {
                        var cashPayment = new CashPaymentMethod();
                        billingService.finalizeBill(currentBill, cashPayment, stockService, BigDecimal.valueOf(10000));
                        req.setAttribute("success", "Bill finalized successfully!");

                        // Store finalized bill separately for display
                        session.setAttribute("finalizedBill", currentBill);
                        session.removeAttribute("currentBill");
                    }
                }
            }
        } catch (Exception e) {
            req.setAttribute("error", e.getMessage());
        }

        req.getRequestDispatcher("counterSales.jsp").forward(req, resp);
    }
    // 🔹 Constructor for injecting mocks in tests only
    public CounterSalesServlet(BillingService billingService, StockManagementService stockService) {
        this.billingService = billingService;
        this.stockService = stockService;
    }

}
