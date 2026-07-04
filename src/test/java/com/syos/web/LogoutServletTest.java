package com.syos.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LogoutServletTest {

    private LogoutServlet logoutServlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        logoutServlet = new LogoutServlet();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    // 1. Test session invalidated if exists
    @Test
    void testSessionInvalidated() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("/app");

        logoutServlet.doGet(request, response);

        verify(session, times(1)).invalidate();
        verify(response).sendRedirect("/app/home");
    }

    // 2. Test no session exists
    @Test
    void testNoSession() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/app");

        assertDoesNotThrow(() -> logoutServlet.doGet(request, response));

        verify(response).sendRedirect("/app/home");
    }

    // 3. Test getSession(false) called once
    @Test
    void testGetSessionCalledOnce() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("/app");

        logoutServlet.doGet(request, response);

        verify(request, times(1)).getSession(false);
    }

    // 4. Test redirect URL
    @Test
    void testRedirectUrl() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("/app");

        logoutServlet.doGet(request, response);

        verify(response).sendRedirect("/app/home");
    }

    // 5. Test invalidate not called if session null
    @Test
    void testInvalidateNotCalledIfNoSession() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/app");

        logoutServlet.doGet(request, response);

        verify(session, never()).invalidate();
    }

    // 6. Test multiple logout calls
    @Test
    void testMultipleLogouts() throws ServletException, IOException {
        HttpSession session2 = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session, session2);
        when(request.getSession()).thenReturn(session, session2);
        when(request.getContextPath()).thenReturn("/app");

        logoutServlet.doGet(request, response);
        logoutServlet.doGet(request, response);

        verify(session).invalidate();
        verify(session2).invalidate();
        verify(response, times(2)).sendRedirect("/app/home");
    }

    // 7. Test null context path
    @Test
    void testNullContextPath() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn(null);

        logoutServlet.doGet(request, response);

        verify(response).sendRedirect("null/home");
    }

    // 8. Test redirect throws IOException
    @Test
    void testRedirectThrowsIOException() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("/app");
        doThrow(new IOException("Redirect failed")).when(response).sendRedirect(anyString());

        assertThrows(IOException.class, () -> logoutServlet.doGet(request, response));
    }

    // 9. Test invalidate called before redirect
    @Test
    void testInvalidateBeforeRedirect() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("/app");

        logoutServlet.doGet(request, response);

        InOrder inOrder = inOrder(session, response);
        inOrder.verify(session).invalidate();
        inOrder.verify(response).sendRedirect("/app/home");
    }

    // 10. Test different sessions invalidated in sequence
    @Test
    void testDifferentSessions() throws ServletException, IOException {
        HttpSession session2 = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session, session2);
        when(request.getSession()).thenReturn(session, session2);
        when(request.getContextPath()).thenReturn("/app");

        logoutServlet.doGet(request, response);
        logoutServlet.doGet(request, response);

        verify(session).invalidate();
        verify(session2).invalidate();
    }
}
