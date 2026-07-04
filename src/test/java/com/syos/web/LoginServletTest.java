package com.syos.web;

import com.syos.application.port.AuthenticationService;
import com.syos.domain.exception.DatabaseOperationException;
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

import java.util.Optional;

import static org.mockito.Mockito.*;

class LoginServletTest {

    private LoginServlet servlet;

    @Mock
    private AuthenticationService authenticationService;

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
        servlet = new LoginServlet();

        // ✅ inject mock service using the setter
        servlet.setAuthenticationService(authenticationService);
    }

    // 1️⃣ Successful login as STAFF → redirect to /employeeMenu
    @Test
    void shouldRedirectToEmployeeMenuOnStaffLogin() throws Exception {
        AuthenticatedUser staffUser = mock(AuthenticatedUser.class);
        when(staffUser.getRole()).thenReturn(UserRole.STAFF);

        when(authenticationService.authenticate("staff1", "pass"))
                .thenReturn(Optional.of(staffUser));
        when(request.getParameter("username")).thenReturn("staff1");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(request.getContextPath()).thenReturn("");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/employeeMenu");
    }

    // 2️⃣ Successful login as CUSTOMER → redirect to /customerMenu
    @Test
    void shouldRedirectToCustomerMenuOnCustomerLogin() throws Exception {
        AuthenticatedUser customerUser = mock(AuthenticatedUser.class);
        when(customerUser.getRole()).thenReturn(UserRole.CUSTOMER);

        when(authenticationService.authenticate("customer1", "pass"))
                .thenReturn(Optional.of(customerUser));
        when(request.getParameter("username")).thenReturn("customer1");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(request.getContextPath()).thenReturn("");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/customerMenu");
    }

    // 3️⃣ Invalid login → forwards back to login.jsp with error
    @Test
    void shouldForwardToLoginJspOnInvalidCredentials() throws Exception {
        when(authenticationService.authenticate("wrong", "wrong"))
                .thenReturn(Optional.empty());
        when(request.getParameter("username")).thenReturn("wrong");
        when(request.getParameter("password")).thenReturn("wrong");
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute("loginError", "Invalid username or password.");
        verify(dispatcher).forward(request, response);
    }

    // 4️⃣ Database error during authentication → forward with DB error message
    @Test
    void shouldForwardToLoginJspOnDatabaseError() throws Exception {
        when(authenticationService.authenticate("user1", "pass"))
                .thenThrow(new DatabaseOperationException("DB down"));
        when(request.getParameter("username")).thenReturn("user1");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("loginError"), contains("Database error"));
        verify(dispatcher).forward(request, response);
    }

    // 5️⃣ GET request → forwards to login.jsp
    @Test
    void shouldForwardToLoginJspOnGet() throws Exception {
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }
    // 1️⃣ Authentication fails → forward to login.jsp with error
    @Test
    void shouldForwardToLoginJspWhenAuthFails() throws Exception {
        when(request.getParameter("username")).thenReturn("wronguser");
        when(request.getParameter("password")).thenReturn("wrongpass");
        when(authenticationService.authenticate("wronguser", "wrongpass"))
                .thenReturn(Optional.empty());
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("loginError"), anyString());
        verify(dispatcher).forward(request, response);
    }

    // 2️⃣ AuthenticationService throws DatabaseOperationException → forward with DB error
    @Test
    void shouldForwardWithDbErrorWhenDbExceptionThrown() throws Exception {
        when(request.getParameter("username")).thenReturn("user");
        when(request.getParameter("password")).thenReturn("pass");
        when(authenticationService.authenticate("user", "pass"))
                .thenThrow(new DatabaseOperationException("DB down"));
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("loginError"), contains("DB down"));
        verify(dispatcher).forward(request, response);
    }
    // 3️⃣ Successful login as STAFF → redirect to /employeeMenu
    @Test
    void shouldRedirectToEmployeeMenuForStaff() throws Exception {
        AuthenticatedUser mockUser = mock(AuthenticatedUser.class);
        when(mockUser.getRole()).thenReturn(UserRole.STAFF);

        when(authenticationService.authenticate("staff", "pass"))
                .thenReturn(Optional.of(mockUser));

        when(request.getParameter("username")).thenReturn("staff");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getContextPath()).thenReturn("");
        // Simulate no previous session
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/employeeMenu");
        verify(session).setAttribute("user", mockUser);
    }

    @Test
    void shouldRedirectToCustomerMenuForCustomer() throws Exception {
        AuthenticatedUser mockUser = mock(AuthenticatedUser.class);
        when(mockUser.getRole()).thenReturn(UserRole.CUSTOMER);

        when(authenticationService.authenticate("cust", "pass"))
                .thenReturn(Optional.of(mockUser));

        when(request.getParameter("username")).thenReturn("cust");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getContextPath()).thenReturn("");
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/customerMenu");
        verify(session).setAttribute("user", mockUser);
    }

    // 5️⃣ Session exists and is invalidated → new session created
    @Test
    void shouldInvalidateOldSessionAndCreateNew() throws Exception {
        var oldSession = mock(HttpSession.class);
        var newSession = mock(HttpSession.class);
        var user = mock(AuthenticatedUser.class);

        when(user.getRole()).thenReturn(UserRole.CUSTOMER);
        when(authenticationService.authenticate("cust", "pass")).thenReturn(Optional.of(user));
        when(request.getParameter("username")).thenReturn("cust");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getSession(false)).thenReturn(oldSession);
        when(request.getSession(true)).thenReturn(newSession);
        when(request.getContextPath()).thenReturn("");

        servlet.doPost(request, response);

        verify(oldSession).invalidate();
        verify(newSession).setAttribute("user", user);
        verify(response).sendRedirect("/customerMenu");
    }

    @Test
    void shouldForwardToLoginOnFailedAuthentication() throws Exception {
        when(authenticationService.authenticate("baduser", "badpass"))
                .thenReturn(Optional.empty());
        when(request.getParameter("username")).thenReturn("baduser");
        when(request.getParameter("password")).thenReturn("badpass");
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("loginError"), eq("Invalid username or password."));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldForwardToLoginOnDatabaseException() throws Exception {
        when(authenticationService.authenticate("any", "any"))
                .thenThrow(new DatabaseOperationException("DB down"));
        when(request.getParameter("username")).thenReturn("any");
        when(request.getParameter("password")).thenReturn("any");
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("loginError"), eq("Database error: DB down"));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void shouldInvalidateOldSessionOnSuccessfulLogin() throws Exception {
        AuthenticatedUser mockUser = mock(AuthenticatedUser.class);
        when(mockUser.getRole()).thenReturn(UserRole.CUSTOMER);

        when(authenticationService.authenticate("cust", "pass"))
                .thenReturn(Optional.of(mockUser));
        when(request.getParameter("username")).thenReturn("cust");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getContextPath()).thenReturn("");
        HttpSession oldSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(oldSession);
        when(request.getSession(true)).thenReturn(newSession);

        servlet.doPost(request, response);

        verify(oldSession).invalidate();
        verify(newSession).setAttribute("user", mockUser);
        verify(response).sendRedirect("/customerMenu");
    }

    @Test
    void shouldRedirectToHomeForUnknownRole() throws Exception {
        AuthenticatedUser mockUser = mock(AuthenticatedUser.class);
        when(mockUser.getRole()).thenReturn(UserRole.ADMIN); // not STAFF or CUSTOMER

        when(authenticationService.authenticate("user", "pass"))
                .thenReturn(Optional.of(mockUser));
        when(request.getParameter("username")).thenReturn("user");
        when(request.getParameter("password")).thenReturn("pass");
        when(request.getContextPath()).thenReturn("");
        when(request.getSession(false)).thenReturn(null);
        HttpSession newSession = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(newSession);

        servlet.doPost(request, response);

        verify(newSession).setAttribute("user", mockUser);
        verify(response).sendRedirect("/home");
    }

    @Test
    void doGetShouldForwardToLoginJsp() throws Exception {
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }


}
