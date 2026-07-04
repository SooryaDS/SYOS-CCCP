package com.syos.web;

import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.enums.UserRole;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerMenuServletConcurrencyTest {

    private CustomerMenuServlet servlet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servlet = new CustomerMenuServlet();
    }

    @Test
    void shouldHandleMultipleCustomersAccessingMenuConcurrently() throws Exception {
        int customerCount = 5; // simulate 5 customers
        CountDownLatch latch = new CountDownLatch(customerCount);

        for (int i = 1; i <= customerCount; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    // Mock request/response/session
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);
                    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

                    AuthenticatedUser user = new AuthenticatedUser(id, "customer" + id, UserRole.CUSTOMER);

                    when(request.getSession(false)).thenReturn(session);
                    when(session.getAttribute("user")).thenReturn(user);
                    when(request.getRequestDispatcher("/customerMenu.jsp")).thenReturn(dispatcher);

                    // Call servlet
                    servlet.doGet(request, response);

                    // Verify each customer gets correct username
                    verify(request).setAttribute("username", "customer" + id);
                    verify(dispatcher).forward(request, response);
                    verify(response, never()).sendRedirect(anyString());

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Exception in concurrent customer access: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all threads to finish
        latch.await();
    }
}
