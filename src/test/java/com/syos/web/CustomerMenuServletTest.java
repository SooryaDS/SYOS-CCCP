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

import static org.mockito.Mockito.*;

class CustomerMenuServletTest {

    private CustomerMenuServlet servlet;

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
        servlet = new CustomerMenuServlet();
    }

    // 1️⃣ No session → redirect to /main
    @Test
    void shouldRedirectToMainWhenUserNotLoggedIn() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/main");
        verifyNoMoreInteractions(response);
    }

    // 2️⃣ Logged in but NOT customer → redirect to /main
    @Test
    void shouldRedirectWhenUserIsNotCustomer() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(1, "adminUser", UserRole.ADMIN);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getContextPath()).thenReturn("");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/main");
    }

    // 3️⃣ Logged in as CUSTOMER → forward to customerMenu.jsp
    @Test
    void shouldForwardToCustomerMenuWhenCustomerLoggedIn() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(2, "customer1", UserRole.CUSTOMER);

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getRequestDispatcher("/customerMenu.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("username", "customer1");
        verify(dispatcher).forward(request, response);
    }
    // 4️⃣ Session exists but user attribute is null → redirect to /main
    @Test
    void shouldRedirectToMainWhenSessionExistsButUserIsNull() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);
        when(request.getContextPath()).thenReturn("");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/main");
    }

    // 5️⃣ Multiple customers → check username is set correctly for each
    @Test
    void shouldSetCorrectUsernameForDifferentCustomer() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(3, "customer2", UserRole.CUSTOMER);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getRequestDispatcher("/customerMenu.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("username", "customer2");
        verify(dispatcher).forward(request, response);
    }

    // 6️⃣ Context path not empty → redirect still works
    @Test
    void shouldRedirectWithContextPath() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/billing-system");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/billing-system/main");
    }

    // 7️⃣ User role is null → redirect to /main
    @Test
    void shouldRedirectWhenUserRoleIsNull() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(4, "userNullRole", null);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getContextPath()).thenReturn("");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/main");
    }

    // 8️⃣ Ensure forward is called only once for a valid customer
    @Test
    void shouldCallForwardOnlyOnceForValidCustomer() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(5, "customer3", UserRole.CUSTOMER);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getRequestDispatcher("/customerMenu.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(dispatcher, times(1)).forward(request, response);
        verify(request, times(1)).setAttribute("username", "customer3");
    }

    //  Session invalidated (returns null) → redirect to /main
    @Test
    void shouldRedirectToMainIfSessionIsInvalidated() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/main");
    }

    //  Logged in as CUSTOMER with empty username → forward with empty username attribute
    @Test
    void shouldForwardWithEmptyUsernameIfCustomerUsernameEmpty() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(6, "", UserRole.CUSTOMER);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getRequestDispatcher("/customerMenu.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("username", "");
        verify(dispatcher).forward(request, response);
    }

    // 12️⃣ Logged in as CUSTOMER, verify no redirect occurs
    @Test
    void shouldNotRedirectWhenCustomerLoggedIn() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(7, "customer4", UserRole.CUSTOMER);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getRequestDispatcher("/customerMenu.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(response, never()).sendRedirect(anyString());
        verify(dispatcher).forward(request, response);
    }

    //  User with a role not equal to CUSTOMER → ensure redirect called exactly once
    @Test
    void shouldRedirectOnceIfRoleNotCustomer() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(8, "staffUser", UserRole.STAFF);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(request.getContextPath()).thenReturn("");

        servlet.doGet(request, response);

        verify(response, times(1)).sendRedirect("/main");
    }


}
