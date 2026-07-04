package com.syos.web;

import com.syos.application.port.OnlineOrderingService;
import com.syos.application.port.StockManagementService;
import com.syos.application.port.OnlineUserService;
import com.syos.domain.enums.UserRole;
import com.syos.domain.exception.*;
import com.syos.domain.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OnlineShopServletConcurrencyTest {

    private OnlineShopServlet servlet;
    private OnlineOrderingService orderingService;
    private StockManagementService stockService;
    private OnlineUserService userService;

    @BeforeEach
    void setUp() {
        servlet = new OnlineShopServlet();

        orderingService = mock(OnlineOrderingService.class);
        stockService = mock(StockManagementService.class);
        userService = mock(OnlineUserService.class);

        servlet.setOnlineOrderingService(orderingService);
        servlet.setStockManagementService(stockService);
        servlet.setOnlineUserService(userService);
    }

    // ------------------ Scenario 1: Multiple users adding items concurrently ------------------
    @Test
    void shouldHandleMultipleUsersAddingItemsConcurrently() throws Exception {
        int userCount = 5;
        CountDownLatch latch = new CountDownLatch(userCount);

        for (int i = 1; i <= userCount; i++) {
            final int userId = i;
            new Thread(() -> {
                try {
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);
                    StringWriter responseWriter = new StringWriter();
                    when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

                    AuthenticatedUser user = mock(AuthenticatedUser.class);
                    when(user.getRole()).thenReturn(UserRole.CUSTOMER);
                    when(user.getId()).thenReturn(userId);
                    when(session.getAttribute("user")).thenReturn(user);
                    when(request.getSession()).thenReturn(session);

                    when(request.getParameter("action")).thenReturn("add");
                    when(request.getParameter("itemCode")).thenReturn("ITEM" + userId);
                    when(request.getParameter("quantity")).thenReturn("1");

                    doNothing().when(orderingService).addItemToActiveOrder(userId, "ITEM" + userId, 1);

                    servlet.doPost(request, response);

                    String output = responseWriter.toString();
                    assertTrue(output.contains("Item added successfully"));

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception in concurrent add thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

    // ------------------ Scenario 2: Multiple users checking out concurrently ------------------
    @Test
    void shouldHandleMultipleUsersCheckoutConcurrently() throws Exception {
        int userCount = 3;
        CountDownLatch latch = new CountDownLatch(userCount);

        for (int i = 1; i <= userCount; i++) {
            final int userId = i;
            new Thread(() -> {
                try {
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);
                    StringWriter responseWriter = new StringWriter();
                    when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

                    AuthenticatedUser user = mock(AuthenticatedUser.class);
                    when(user.getRole()).thenReturn(UserRole.CUSTOMER);
                    when(user.getId()).thenReturn(userId);
                    when(session.getAttribute("user")).thenReturn(user);
                    when(request.getSession()).thenReturn(session);

                    when(request.getParameter("action")).thenReturn("checkout");
                    when(request.getParameter("shippingAddress")).thenReturn("Address " + userId);

                    Bill bill = mock(Bill.class);
                    when(orderingService.checkoutOrder(userId, "Address " + userId)).thenReturn(bill);
                    when(bill.generateReceipt()).thenReturn("RECEIPT-" + userId);

                    servlet.doPost(request, response);

                    String output = responseWriter.toString();
                    assertTrue(output.contains("Checkout successful"));
                    assertTrue(output.contains("RECEIPT-" + userId));

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception in concurrent checkout thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

    // ------------------ Scenario 3: Single user adding same item concurrently ------------------
    @Test
    void shouldHandleSingleUserAddingSameItemConcurrently() throws Exception {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);

        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(user.getId()).thenReturn(42);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);
                    StringWriter responseWriter = new StringWriter();
                    when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
                    when(session.getAttribute("user")).thenReturn(user);
                    when(request.getSession()).thenReturn(session);

                    when(request.getParameter("action")).thenReturn("add");
                    when(request.getParameter("quantity")).thenReturn("1");
                    when(request.getParameter("itemCode")).thenReturn("ITEM42");

                    doNothing().when(orderingService).addItemToActiveOrder(42, "ITEM42", 1);

                    servlet.doPost(request, response);

                    String output = responseWriter.toString();
                    assertTrue(output.contains("Item added successfully") || output.contains("Quantity updated successfully"));

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception in concurrent thread: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }
}
