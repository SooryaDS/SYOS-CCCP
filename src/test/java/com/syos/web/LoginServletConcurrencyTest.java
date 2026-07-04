package com.syos.web;

import com.syos.application.port.AuthenticationService;
import com.syos.domain.enums.UserRole;
import com.syos.domain.model.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

class LoginServletConcurrencyTest {

    private LoginServlet servlet;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        servlet = new LoginServlet();
        authenticationService = mock(AuthenticationService.class);
        servlet.setAuthenticationService(authenticationService);
    }

    @Test
    void shouldHandleConcurrentLogins() throws Exception {
        int threadCount = 10; // simulate 10 concurrent logins
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Mock request/response/session for each thread
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    HttpServletRequest req = mock(HttpServletRequest.class);
                    HttpServletResponse resp = mock(HttpServletResponse.class);
                    HttpSession session = mock(HttpSession.class);

                    when(req.getParameter("username")).thenReturn("user");
                    when(req.getParameter("password")).thenReturn("pass");
                    when(req.getSession(false)).thenReturn(null);
                    when(req.getSession(true)).thenReturn(session);
                    when(req.getContextPath()).thenReturn("");

                    AuthenticatedUser user = mock(AuthenticatedUser.class);
                    when(user.getRole()).thenReturn(UserRole.CUSTOMER);
                    when(authenticationService.authenticate("user", "pass")).thenReturn(Optional.of(user));

                    servlet.doPost(req, resp);

                    // Verify session attributes and redirect
                    verify(session).setAttribute("user", user);
                    verify(resp).sendRedirect("/customerMenu");

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all threads to finish
        latch.await();
    }
}
