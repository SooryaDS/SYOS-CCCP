package com.syos.web;

import com.syos.domain.enums.UserRole;
import com.syos.domain.model.AuthenticatedUser;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

class MainMenuServletTest {

    private MainMenuServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        servlet = new MainMenuServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);

        when(request.getSession()).thenReturn(session);
    }

    // 1. User not logged in → forward to mainmenu.jsp
    @Test
    void testUserNullForwardsToMainMenu() throws ServletException, IOException {
        when(session.getAttribute("user")).thenReturn(null);
        when(request.getRequestDispatcher("mainmenu.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
        verify(response, never()).sendRedirect(anyString());
    }

    // 2. User is STAFF → redirect to /employeeMenu
    @Test
    void testStaffRedirectsToEmployeeMenu() throws ServletException, IOException {
        AuthenticatedUser staffUser = mock(AuthenticatedUser.class);
        when(staffUser.getRole()).thenReturn(UserRole.STAFF);
        when(session.getAttribute("user")).thenReturn(staffUser);
        when(request.getContextPath()).thenReturn("/app");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/app/employeeMenu");
    }

    // 3. User is CUSTOMER → redirect to /customerMenu
    @Test
    void testCustomerRedirectsToCustomerMenu() throws ServletException, IOException {
        AuthenticatedUser customerUser = mock(AuthenticatedUser.class);
        when(customerUser.getRole()).thenReturn(UserRole.CUSTOMER);
        when(session.getAttribute("user")).thenReturn(customerUser);
        when(request.getContextPath()).thenReturn("/app");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/app/customerMenu");
    }

    // 4. User has invalid role → send 403 error
    @Test
    void testInvalidRoleSendsForbidden() throws ServletException, IOException {
        AuthenticatedUser invalidUser = mock(AuthenticatedUser.class);
        when(invalidUser.getRole()).thenReturn(null);
        when(session.getAttribute("user")).thenReturn(invalidUser);

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "No valid role found.");
    }

}
