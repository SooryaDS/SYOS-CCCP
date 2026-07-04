package com.syos.web;

import com.syos.application.port.BillingService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.model.Bill;
import com.syos.service.payment.CashPaymentMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CounterSalesServletConcurrencyTest {

    private CounterSalesServlet servlet;

    @Mock
    private BillingService billingService;

    @Mock
    private StockManagementService stockService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servlet = new CounterSalesServlet(billingService, stockService);
    }

    // ------------------ Scenario 1: Multiple cashiers starting new bills concurrently ------------------
    @Test
    void shouldHandleMultipleCashiersStartingNewBills() throws Exception {
        int cashierCount = 5;
        CountDownLatch latch = new CountDownLatch(cashierCount);

        for (int i = 1; i <= cashierCount; i++) {
            final int cashierId = i;
            new Thread(() -> {
                try {
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);
                    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
                    StringWriter writer = new StringWriter();
                    when(response.getWriter()).thenReturn(new PrintWriter(writer));
                    when(request.getSession(anyBoolean())).thenReturn(session);
                    when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
                    when(request.getParameter("action")).thenReturn("Start New Bill");

                    Bill bill = new Bill();
                    when(billingService.startNewBill(TransactionType.POS_CASH)).thenReturn(bill);

                    servlet.doPost(request, response);

                    verify(session).setAttribute("currentBill", bill);
                    verify(dispatcher).forward(request, response);

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception in concurrent start new bill thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

    // ------------------ Scenario 2: Multiple cashiers adding items concurrently ------------------
    @Test
    void shouldHandleMultipleCashiersAddingItemsConcurrently() throws Exception {
        int cashierCount = 3;
        CountDownLatch latch = new CountDownLatch(cashierCount);

        for (int i = 1; i <= cashierCount; i++) {
            final int cashierId = i;
            new Thread(() -> {
                try {
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);
                    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
                    StringWriter writer = new StringWriter();
                    when(response.getWriter()).thenReturn(new PrintWriter(writer));
                    when(request.getSession(anyBoolean())).thenReturn(session);
                    when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
                    when(request.getParameter("action")).thenReturn("Add Item");
                    when(request.getParameter("itemCode")).thenReturn("ITEM00" + cashierId);
                    when(request.getParameter("quantity")).thenReturn("2");

                    Bill bill = new Bill();
                    when(session.getAttribute("currentBill")).thenReturn(bill);

                    doNothing().when(billingService).addItemToBill(bill, "ITEM00" + cashierId, 2);

                    servlet.doPost(request, response);

                    verify(billingService).addItemToBill(bill, "ITEM00" + cashierId, 2);
                    verify(session).setAttribute("currentBill", bill);
                    verify(dispatcher).forward(request, response);

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception in concurrent add item thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

    // ------------------ Scenario 3: Multiple cashiers finalizing bills concurrently ------------------
    @Test
    void shouldHandleMultipleCashiersFinalizingBills() throws Exception {
        int cashierCount = 3;
        CountDownLatch latch = new CountDownLatch(cashierCount);

        for (int i = 1; i <= cashierCount; i++) {
            final int cashierId = i;
            new Thread(() -> {
                try {
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);
                    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
                    StringWriter writer = new StringWriter();
                    when(response.getWriter()).thenReturn(new PrintWriter(writer));
                    when(request.getSession(anyBoolean())).thenReturn(session);
                    when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
                    when(request.getParameter("action")).thenReturn("Finalize Bill");

                    Bill bill = new Bill();
                    when(session.getAttribute("currentBill")).thenReturn(bill);

                    doNothing().when(billingService)
                            .finalizeBill(eq(bill), any(CashPaymentMethod.class), eq(stockService), eq(BigDecimal.valueOf(10000)));

                    servlet.doPost(request, response);

                    verify(billingService)
                            .finalizeBill(eq(bill), any(CashPaymentMethod.class), eq(stockService), eq(BigDecimal.valueOf(10000)));
                    verify(session).setAttribute("finalizedBill", bill);
                    verify(session).removeAttribute("currentBill");
                    verify(request).setAttribute("success", "Bill finalized successfully!");
                    verify(dispatcher).forward(request, response);

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception in concurrent finalize bill thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }
}
