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
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OnlineShopServletTest {

    private OnlineShopServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private OnlineOrderingService orderingService;
    private StockManagementService stockService;
    private OnlineUserService userService;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new OnlineShopServlet();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        orderingService = mock(OnlineOrderingService.class);
        stockService = mock(StockManagementService.class);
        userService = mock(OnlineUserService.class);

        // inject mocks using servlet setters
        servlet.setOnlineOrderingService(orderingService);
        servlet.setStockManagementService(stockService);
        servlet.setOnlineUserService(userService);

        when(request.getSession()).thenReturn(session);

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    // ------------------ doGet tests ------------------

    @Test
    void testDoGetUserNull() throws Exception {
        when(session.getAttribute("user")).thenReturn(null);

        servlet.doGet(request, response);

        assertTrue(responseWriter.toString().contains("Please <a href='login'>login as a customer</a>"));
    }

    @Test
    void testDoGetUserNotCustomer() throws Exception {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.STAFF);
        when(session.getAttribute("user")).thenReturn(user);

        servlet.doGet(request, response);

        assertTrue(responseWriter.toString().contains("Please <a href='login'>login as a customer</a>"));
    }

    @Test
    void testDoGetDatabaseException() throws Exception {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(user.getUsername()).thenReturn("Alice");
        when(session.getAttribute("user")).thenReturn(user);

        when(orderingService.getActiveOrderForUser(anyInt())).thenThrow(new DatabaseOperationException("DB error"));
        when(stockService.getAllWebsiteStock()).thenThrow(new DatabaseOperationException("DB error"));

        servlet.doGet(request, response);

        assertTrue(responseWriter.toString().contains("Error fetching data: DB error"));
    }

    @Test
    void testDoGetActiveOrderAndWebsiteStock() throws Exception {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(user.getUsername()).thenReturn("Alice");
        when(user.getId()).thenReturn(1);
        when(session.getAttribute("user")).thenReturn(user);

        // MOCK domain objects instead of calling constructors
        Item item = mock(Item.class);
        when(item.getItemCode()).thenReturn("I001");
        when(item.getName()).thenReturn("Test Item");
        when(item.getUnitPrice()).thenReturn(BigDecimal.valueOf(10));

        OnlineOrderItem orderItem = mock(OnlineOrderItem.class);
        when(orderItem.getItem()).thenReturn(item);
        when(orderItem.getQuantity()).thenReturn(2);
        when(orderItem.getPriceAtAddition()).thenReturn(BigDecimal.valueOf(10));
        when(orderItem.getLineTotal()).thenReturn(BigDecimal.valueOf(20));

        OnlineOrder order = mock(OnlineOrder.class);
        when(user.getId()).thenReturn(Integer.valueOf(1));

        when(order.getItems()).thenReturn(List.of(orderItem));
        when(order.getCalculatedTotalAmount()).thenReturn(BigDecimal.valueOf(20));

        WebsiteStock stock = mock(WebsiteStock.class);
        when(stock.getItem()).thenReturn(item);
        when(stock.getQuantityAvailableOnline()).thenReturn(5);

        when(orderingService.getActiveOrderForUser(anyInt())).thenReturn(order);
        when(stockService.getAllWebsiteStock()).thenReturn(List.of(stock));

        servlet.doGet(request, response);

        String output = responseWriter.toString();
        assertTrue(output.contains("Welcome, Alice"));
        assertTrue(output.contains("Current Order"));
        assertTrue(output.contains("Test Item"));
        assertTrue(output.contains("<option value='I001'>Test Item"));
    }

    @Test
    void testDoGetNoActiveOrder() throws Exception {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(user.getUsername()).thenReturn("Alice");
        when(user.getId()).thenReturn(1);
        when(session.getAttribute("user")).thenReturn(user);

        OnlineOrder emptyOrder = mock(OnlineOrder.class);
        when(emptyOrder.getItems()).thenReturn(Collections.emptyList());

        when(orderingService.getActiveOrderForUser(anyInt())).thenReturn(emptyOrder);
        when(stockService.getAllWebsiteStock()).thenReturn(Collections.emptyList());

        servlet.doGet(request, response);

        assertTrue(responseWriter.toString().contains("No active order yet"));
    }

    // ------------------ doPost tests ------------------

    @Test
    void testDoPostUserNull() throws Exception {
        when(session.getAttribute("user")).thenReturn(null);

        servlet.doPost(request, response);

        assertTrue(responseWriter.toString().contains("Please <a href='login'>login as a customer</a>"));
    }

    @Test
    void testDoPostInvalidQuantity() throws Exception {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(session.getAttribute("user")).thenReturn(user);

        when(request.getParameter("quantity")).thenReturn("abc"); // invalid
        when(request.getParameter("action")).thenReturn("add");

        servlet.doPost(request, response);

        assertTrue(responseWriter.toString().contains("Invalid quantity"));
    }

    @Test
    void testDoPostAddUpdateRemoveCheckout() throws Exception {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(user.getId()).thenReturn(1);
        when(session.getAttribute("user")).thenReturn(user);

        when(request.getParameter("quantity")).thenReturn("2");
        when(request.getParameter("itemCode")).thenReturn("I001");
        when(request.getParameter("shippingAddress")).thenReturn("123 Street");

        // ADD
        when(request.getParameter("action")).thenReturn("add");
        servlet.doPost(request, response);
        assertTrue(responseWriter.toString().contains("Item added successfully"));

        // UPDATE
        responseWriter.getBuffer().setLength(0);
        when(request.getParameter("action")).thenReturn("update");
        servlet.doPost(request, response);
        assertTrue(responseWriter.toString().contains("Quantity updated successfully"));

        // REMOVE
        responseWriter.getBuffer().setLength(0);
        when(request.getParameter("action")).thenReturn("remove");
        servlet.doPost(request, response);
        assertTrue(responseWriter.toString().contains("Item removed successfully"));

        // CHECKOUT
        responseWriter.getBuffer().setLength(0);
        Bill bill = mock(Bill.class);
        when(orderingService.checkoutOrder(1, "123 Street")).thenReturn(bill);
        when(bill.generateReceipt()).thenReturn("RECEIPT");

        when(request.getParameter("action")).thenReturn("checkout");
        servlet.doPost(request, response);
        assertTrue(responseWriter.toString().contains("Checkout successful"));
        assertTrue(responseWriter.toString().contains("RECEIPT"));

        // UNKNOWN action
        responseWriter.getBuffer().setLength(0);
        when(request.getParameter("action")).thenReturn("unknown");
        servlet.doPost(request, response);
        assertTrue(responseWriter.toString().contains("Unknown action"));
    }

    @Test
    void testDoPostThrowsExceptions() throws Exception {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(user.getId()).thenReturn(1);
        when(session.getAttribute("user")).thenReturn(user);

        when(request.getParameter("quantity")).thenReturn("1");
        when(request.getParameter("itemCode")).thenReturn("I001");
        when(request.getParameter("action")).thenReturn("add");

        doThrow(new ItemNotFoundException("Item missing")).when(orderingService).addItemToActiveOrder(1, "I001", 1);

        servlet.doPost(request, response);

        assertTrue(responseWriter.toString().contains("Error: Item missing"));
    }
}
