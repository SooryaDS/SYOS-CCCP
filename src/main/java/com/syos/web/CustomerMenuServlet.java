package com.syos.web;

import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.enums.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/customerMenu")
public class CustomerMenuServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        AuthenticatedUser user = (AuthenticatedUser) (session != null ? session.getAttribute("user") : null);

        if (user == null) {
            System.out.println("DEBUG: No user in session, redirecting to /main");
            resp.sendRedirect(req.getContextPath() + "/main");
            return;
        }

        System.out.println("DEBUG: Logged in as " + user.getUsername() + " with role " + user.getRole());

        if (!UserRole.CUSTOMER.equals(user.getRole())) {
            System.out.println("DEBUG: Not a customer, redirecting to /main");
            resp.sendRedirect(req.getContextPath() + "/main");
            return;
        }

        req.setAttribute("username", user.getUsername());
        req.getRequestDispatcher("/customerMenu.jsp").forward(req, resp);
    }
}
