package com.syos.web;

import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.enums.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/main")
public class MainMenuServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        AuthenticatedUser user = (AuthenticatedUser) session.getAttribute("user");

        if (user == null) {
            // Not logged in → show login/register page
            req.getRequestDispatcher("mainmenu.jsp").forward(req, resp);
        } else {
            // Logged in → redirect based on role
            if (user.getRole() == UserRole.STAFF) {
                resp.sendRedirect(req.getContextPath() + "/employeeMenu");
            } else if (user.getRole() == UserRole.CUSTOMER) {
                resp.sendRedirect(req.getContextPath() + "/customerMenu");
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "No valid role found.");
            }
        }
    }
}
