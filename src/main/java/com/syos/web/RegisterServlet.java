package com.syos.web;

import com.syos.application.port.OnlineUserService;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;
import com.syos.domain.model.OnlineUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private OnlineUserService onlineUserService;

    public RegisterServlet() {
        // default constructor: get service from AppContext
        this.onlineUserService = AppContext.getInstance().getOnlineUserService();
    }

    // setter to inject mock service in tests
    public void setOnlineUserService(OnlineUserService svc) {
        this.onlineUserService = svc;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.getWriter().println(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><title>Register</title></head>" +
                        "<body>" +
                        "<h2>Register New Account</h2>" +
                        "<form method='post'>" +
                        "Username: <input type='text' name='username' required><br>" +
                        "Password: <input type='password' name='password' required><br>" +
                        "Full Name: <input type='text' name='fullName'><br>" +
                        "Email: <input type='email' name='email' required><br>" +
                        "Address: <input type='text' name='address'><br>" +
                        "<input type='submit' value='Register'>" +
                        "</form>" +
                        "<p><a href='/login'>Already have an account? Login here</a></p>" +
                        "</body>" +
                        "</html>"
        );
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String address = req.getParameter("address");

        try {
            OnlineUser user = onlineUserService.registerUser(username, password, fullName, email, address);
            resp.setContentType("text/html");
            resp.getWriter().println("<p>Registration successful! Welcome, " + user.getUsername() + ".</p>");
            resp.getWriter().println("<p><a href='/login'>Go to Login</a></p>");
        } catch (UserAlreadyExistsException e) {
            resp.setContentType("text/html");
            resp.getWriter().println("<p style='color:red;'>Error: Username already exists.</p>");
            doGet(req, resp);
        } catch (DatabaseOperationException e) {
            resp.setContentType("text/html");
            resp.getWriter().println("<p style='color:red;'>Database error occurred. Please try again later.</p>");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            resp.setContentType("text/html");
            resp.getWriter().println("<p style='color:red;'>Invalid input: " + e.getMessage() + "</p>");
            doGet(req, resp);
        }
    }
}
