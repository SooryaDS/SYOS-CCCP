package com.syos.web;

import com.syos.application.port.BillingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.model.Bill;
import com.syos.service.payment.CashPaymentMethod;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

class CounterSalesServletTest {

    private CounterSalesServlet servlet;

    @Mock
    private BillingService billingService;

    @Mock
    private StockManagementService stockService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servlet = new CounterSalesServlet(billingService, stockService);

        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    //  GET request forwards to counterSales.jsp
    @Test
    void shouldForwardToCounterSalesOnGet() throws ServletException, IOException {
        servlet.doGet(request, response);
        verify(request).getRequestDispatcher("/counterSales.jsp");
        verify(dispatcher).forward(request, response);
    }

    //  Start new bill sets session attribute
    @Test
    void shouldStartNewBill() throws ServletException, IOException {
        Bill bill = new Bill();
        when(billingService.startNewBill(TransactionType.POS_CASH)).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Start New Bill");

        servlet.doPost(request, response);

        verify(session).setAttribute("currentBill", bill);
        verify(dispatcher).forward(request, response);
    }

    //  Add item successfully
    @Test
    void shouldAddItemToBill() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Add Item");
        when(request.getParameter("itemCode")).thenReturn("ITEM001");
        when(request.getParameter("quantity")).thenReturn("2");

        servlet.doPost(request, response);

        verify(billingService).addItemToBill(bill, "ITEM001", 2);
        verify(session).setAttribute("currentBill", bill);
        verify(dispatcher).forward(request, response);
    }

    //  Add item without starting bill → set error
    @Test
    void shouldShowErrorIfAddingItemWithoutBill() throws Exception {
        when(session.getAttribute("currentBill")).thenReturn(null);
        when(request.getParameter("action")).thenReturn("Add Item");

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
    }

    //  Finalize bill successfully
    @Test
    void shouldFinalizeBill() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Finalize Bill");

        servlet.doPost(request, response);

        verify(billingService).finalizeBill(eq(bill), any(CashPaymentMethod.class), eq(stockService), eq(BigDecimal.valueOf(10000)));
        verify(request).setAttribute("success", "Bill finalized successfully!");
        verify(session).setAttribute("finalizedBill", bill);
        verify(session).removeAttribute("currentBill");
        verify(dispatcher).forward(request, response);
    }

    //  Finalize bill without starting → error
    @Test
    void shouldShowErrorWhenFinalizingWithoutBill() throws Exception {
        when(session.getAttribute("currentBill")).thenReturn(null);
        when(request.getParameter("action")).thenReturn("Finalize Bill");

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
    }

    //  Start new bill → exception thrown in billingService
    @Test
    void shouldHandleExceptionOnStartNewBill() throws Exception {
        when(request.getParameter("action")).thenReturn("Start New Bill");
        when(billingService.startNewBill(TransactionType.POS_CASH))
                .thenThrow(new RuntimeException("DB error"));

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "DB error");
        verify(dispatcher).forward(request, response);
    }

    // Add item → exception thrown
    @Test
    void shouldHandleExceptionOnAddItem() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Add Item");
        when(request.getParameter("itemCode")).thenReturn("ITEM001");
        when(request.getParameter("quantity")).thenReturn("2");
        doThrow(new RuntimeException("Item error")).when(billingService).addItemToBill(bill, "ITEM001", 2);

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Item error");
        verify(dispatcher).forward(request, response);
    }

    // finalize bill → exception thrown
    @Test
    void shouldHandleExceptionOnFinalizeBill() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Finalize Bill");
        doThrow(new RuntimeException("Finalize error"))
                .when(billingService).finalizeBill(eq(bill), any(CashPaymentMethod.class), eq(stockService), eq(BigDecimal.valueOf(10000)));

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Finalize error");
        verify(dispatcher).forward(request, response);
    }

    // Unknown action → should just forward
    @Test
    void shouldForwardOnUnknownAction() throws Exception {
        when(request.getParameter("action")).thenReturn("UNKNOWN_ACTION");

        servlet.doPost(request, response);

        verify(dispatcher).forward(request, response);
    }
    // 5 Finalize bill → stockService throws exception
    @Test
    void shouldHandleStockServiceExceptionOnFinalize() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Finalize Bill");

        doThrow(new RuntimeException("Stock error"))
                .when(billingService)
                .finalizeBill(eq(bill), any(CashPaymentMethod.class), eq(stockService), eq(BigDecimal.valueOf(10000)));

        servlet.doPost(request, response);

        verify(request).setAttribute("error", "Stock error");
        verify(dispatcher).forward(request, response);
    }


    @Test
    void shouldStartNewBillWithNoOldSession() throws Exception {
        Bill bill = new Bill();
        when(billingService.startNewBill(TransactionType.POS_CASH)).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Start New Bill");
        when(request.getSession(false)).thenReturn(null);

        servlet.doPost(request, response);

        verify(session).setAttribute("currentBill", bill);
        verify(dispatcher).forward(request, response);
    }

    //  Multiple actions → simulate sequence: start, add, finalize
    @Test
    void shouldHandleMultipleActionsInSequence() throws Exception {
        Bill bill = new Bill();
        when(billingService.startNewBill(TransactionType.POS_CASH)).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Start New Bill");
        servlet.doPost(request, response);

        when(request.getParameter("action")).thenReturn("Add Item");
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("itemCode")).thenReturn("ITEM002");
        when(request.getParameter("quantity")).thenReturn("1");
        servlet.doPost(request, response);

        when(request.getParameter("action")).thenReturn("Finalize Bill");
        servlet.doPost(request, response);

        verify(billingService).addItemToBill(bill, "ITEM002", 1);
        verify(billingService).finalizeBill(eq(bill), any(CashPaymentMethod.class), eq(stockService), eq(BigDecimal.valueOf(10000)));
    }

    //  Finalize bill → session is null → error
    @Test
    void shouldSetErrorIfFinalizeWithoutSession() throws ServletException, IOException {
        when(request.getSession(true)).thenReturn(session);
        when(session.getAttribute("currentBill")).thenReturn(null);
        when(request.getParameter("action")).thenReturn("Finalize Bill");

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("error"), anyString());
        verify(dispatcher).forward(request, response);
    }

    // Unknown action → null currentBill → just forward
    @Test
    void shouldForwardWithNullBillAndUnknownAction() throws ServletException, IOException {
        when(session.getAttribute("currentBill")).thenReturn(null);
        when(request.getParameter("action")).thenReturn("UNKNOWN");

        servlet.doPost(request, response);

        verify(dispatcher).forward(request, response);
    }
    @Test
    void shouldOverwriteCurrentBillWhenStartingNewBill() throws Exception {
        Bill oldBill = new Bill();
        Bill newBill = new Bill();

        when(session.getAttribute("currentBill")).thenReturn(oldBill);
        when(request.getParameter("action")).thenReturn("Start New Bill");
        when(billingService.startNewBill(TransactionType.POS_CASH)).thenReturn(newBill);

        servlet.doPost(request, response);

        verify(session).setAttribute("currentBill", newBill);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldHandleInvalidQuantityWhenAddingItem() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Add Item");
        when(request.getParameter("itemCode")).thenReturn("ITEM001");
        when(request.getParameter("quantity")).thenReturn("invalid"); // not an int

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("error"), contains("For input string"));
        verify(dispatcher).forward(request, response);
    }


    @Test
    void shouldHandleExceptionWhenAddingItem() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Add Item");
        when(request.getParameter("itemCode")).thenReturn("ITEM002");
        when(request.getParameter("quantity")).thenReturn("1");

        doThrow(new RuntimeException("Custom DB error"))
                .when(billingService).addItemToBill(bill, "ITEM002", 1);

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("error"), eq("Custom DB error"));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldFinalizeBillSuccessfully() throws Exception {
        Bill bill = new Bill();
        when(session.getAttribute("currentBill")).thenReturn(bill);
        when(request.getParameter("action")).thenReturn("Finalize Bill");

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("success"), eq("Bill finalized successfully!"));
        verify(session).setAttribute("finalizedBill", bill);
        verify(session).removeAttribute("currentBill");
        verify(dispatcher).forward(request, response);
    }


}
