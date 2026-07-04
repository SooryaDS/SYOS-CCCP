package com.syos.web;

import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.enums.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/employeeMenu")
public class EmployeeMenuServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Get existing session if available (don’t create a new one)
        HttpSession session = req.getSession(false);
        AuthenticatedUser user = (session != null)
                ? (AuthenticatedUser) session.getAttribute("user")
                : null;

        // Check if user is logged in and has the STAFF role
        if (user == null) {
            System.out.println("DEBUG: No user found in session. Redirecting to /main.");
            resp.sendRedirect(req.getContextPath() + "/main");
            return;
        }

        System.out.println("DEBUG: Logged in as " + user.getUsername() + " with role " + user.getRole());

        if (!UserRole.STAFF.equals(user.getRole())) {
            System.out.println("DEBUG: Unauthorized access. User is not STAFF. Redirecting to /main.");
            resp.sendRedirect(req.getContextPath() + "/main");
            return;
        }

        // Pass username to JSP
        req.setAttribute("username", user.getUsername());

        // Forward to JSP (ensure the JSP is in correct location)
        req.getRequestDispatcher("/employeeMenu.jsp").forward(req, resp);
    }
}
