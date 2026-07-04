package com.syos.web;

import com.syos.application.port.OnlineUserService;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;
import com.syos.domain.model.OnlineUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RegisterServletTest {

    private RegisterServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private OnlineUserService userService;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new RegisterServlet();

        // create mocks
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        userService = mock(OnlineUserService.class);

        // inject mock service into servlet
        servlet.setOnlineUserService(userService);

        // capture response output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    // ------------------ doGet tests ------------------

    @Test
    void testDoGet() throws Exception {
        servlet.doGet(request, response);
        String output = responseWriter.toString();
        assertTrue(output.contains("<h2>Register New Account</h2>"));
        assertTrue(output.contains("<form method='post'>"));
        assertTrue(output.contains("Username: <input type='text' name='username' required>"));
    }

    // ------------------ doPost tests ------------------

    @Test
    void testDoPostSuccess() throws Exception {
        when(request.getParameter("username")).thenReturn("alice");
        when(request.getParameter("password")).thenReturn("password");
        when(request.getParameter("fullName")).thenReturn("Alice A");
        when(request.getParameter("email")).thenReturn("alice@example.com");
        when(request.getParameter("address")).thenReturn("123 Street");

        OnlineUser user = mock(OnlineUser.class);
        when(user.getUsername()).thenReturn("alice");

        when(userService.registerUser("alice","password","Alice A","alice@example.com","123 Street"))
                .thenReturn(user);

        servlet.doPost(request, response);

        String output = responseWriter.toString();
        assertTrue(output.contains("Registration successful"));
        assertTrue(output.contains("Welcome, alice"));
    }

    @Test
    void testDoPostUserAlreadyExists() throws Exception {
        when(request.getParameter("username")).thenReturn("alice");
        when(request.getParameter("password")).thenReturn("password");
        when(request.getParameter("fullName")).thenReturn("Alice A");
        when(request.getParameter("email")).thenReturn("alice@example.com");
        when(request.getParameter("address")).thenReturn("123 Street");

        when(userService.registerUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new UserAlreadyExistsException("User exists"));

        servlet.doPost(request, response);

        String output = responseWriter.toString();
        assertTrue(output.contains("Error: Username already exists"));
        assertTrue(output.contains("<h2>Register New Account</h2>")); // doGet called
    }

    @Test
    void testDoPostDatabaseError() throws Exception {
        when(request.getParameter("username")).thenReturn("alice");
        when(request.getParameter("password")).thenReturn("password");
        when(request.getParameter("fullName")).thenReturn("Alice A");
        when(request.getParameter("email")).thenReturn("alice@example.com");
        when(request.getParameter("address")).thenReturn("123 Street");

        when(userService.registerUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DatabaseOperationException("DB error"));

        servlet.doPost(request, response);

        String output = responseWriter.toString();
        assertTrue(output.contains("Database error occurred"));
    }

    @Test
    void testDoPostIllegalArgument() throws Exception {
        when(request.getParameter("username")).thenReturn("alice");
        when(request.getParameter("password")).thenReturn("password");
        when(request.getParameter("fullName")).thenReturn("Alice A");
        when(request.getParameter("email")).thenReturn("alice@example.com");
        when(request.getParameter("address")).thenReturn("123 Street");

        when(userService.registerUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid username"));

        servlet.doPost(request, response);

        String output = responseWriter.toString();
        assertTrue(output.contains("Invalid input: Invalid username"));
        assertTrue(output.contains("<h2>Register New Account</h2>")); // doGet called
    }
}
