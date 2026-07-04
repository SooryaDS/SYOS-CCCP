package com.syos.web;

import com.syos.application.port.AuthenticationService;
import com.syos.domain.exception.DatabaseOperationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;


public class LoginServlet extends HttpServlet {

    private AuthenticationService authenticationService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.authenticationService = AppContext.getInstance().getAuthenticationService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // ✅ Forward to JSP page (no HTML in Java)
        req.getRequestDispatcher("login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            authenticationService.authenticate(username, password).ifPresentOrElse(
                    authenticatedUser -> {
                        // Clear old session if any
                        HttpSession oldSession = req.getSession(false);
                        if (oldSession != null) {
                            oldSession.invalidate();
                        }

                        // Create a fresh session
                        HttpSession newSession = req.getSession(true);
                        newSession.setAttribute("user", authenticatedUser);

                        try {
                            // Redirect based on role
                            String redirectUrl = switch (authenticatedUser.getRole()) {
                                case STAFF -> req.getContextPath() + "/employeeMenu";
                                case CUSTOMER -> req.getContextPath() + "/customerMenu";
                                default -> req.getContextPath() + "/home";
                            };
                            resp.sendRedirect(redirectUrl);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    },
                    () -> {
                        // Authentication failed — show error on JSP
                        req.setAttribute("loginError", "Invalid username or password.");
                        try {
                            req.getRequestDispatcher("login.jsp").forward(req, resp);
                        } catch (IOException | ServletException e) {
                            e.printStackTrace();
                        }
                    }
            );
        } catch (DatabaseOperationException e) {
            req.setAttribute("loginError", "Database error: " + e.getMessage());
            req.getRequestDispatcher("login.jsp").forward(req, resp);
        }
    }
    // 🔹 Add this method for testing only
// Allows injecting a mock AuthenticationService
    void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }



}
