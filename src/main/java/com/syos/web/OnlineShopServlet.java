package com.syos.web;

import com.syos.application.port.OnlineOrderingService;
import com.syos.application.port.OnlineUserService;
import com.syos.application.port.StockManagementService;
import com.syos.domain.exception.*;
import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.model.Bill;
import com.syos.domain.model.Item;
import com.syos.domain.model.OnlineOrder;
import com.syos.domain.model.OnlineOrderItem;
import com.syos.domain.model.WebsiteStock;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/shop")
public class OnlineShopServlet extends HttpServlet {

    private OnlineOrderingService onlineOrderingService;
    private OnlineUserService onlineUserService;
    private StockManagementService stockManagementService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.onlineOrderingService = AppContext.getInstance().getOnlineOrderingService();
        this.onlineUserService = AppContext.getInstance().getOnlineUserService();
        this.stockManagementService = AppContext.getInstance().getStockManagementService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        AuthenticatedUser user = (AuthenticatedUser) req.getSession().getAttribute("user");
        if (user == null || user.getRole() != com.syos.domain.enums.UserRole.CUSTOMER) {
            out.println("<p>Please <a href='login'>login as a customer</a>.</p>");
            return;
        }

        OnlineOrder currentOrder = null;
        List<WebsiteStock> websiteStocks = null;

        try {
            currentOrder = onlineOrderingService.getActiveOrderForUser(user.getId());
            websiteStocks = stockManagementService.getAllWebsiteStock(); // ✅ returns WebsiteStock list
        } catch (DatabaseOperationException e) {
            out.println("<p>Error fetching data: " + e.getMessage() + "</p>");
        }

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Online Shop</title>");
        out.println("<link rel='stylesheet' href='css/shop.css'>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("<h2>Welcome, " + user.getUsername() + "!</h2>");
        out.println("<p>Online Shopping Portal</p>");

        // -------------------- ORDER TABLE --------------------
        if (currentOrder != null && !currentOrder.getItems().isEmpty()) {
            out.println("<div class='order-summary'>");
            out.println("<h3>Current Order (ID: " + currentOrder.getOrderId() + ")</h3>");
            out.println("<table>");
            out.println("<tr><th>Item Code</th><th>Name</th><th>Quantity</th><th>Unit Price</th><th>Subtotal</th></tr>");
            for (OnlineOrderItem item : currentOrder.getItems()) {
                out.println("<tr>");
                out.println("<td>" + item.getItem().getItemCode() + "</td>");
                out.println("<td>" + item.getItem().getName() + "</td>");
                out.println("<td>" + item.getQuantity() + "</td>");
                out.println("<td>" + formatPrice(item.getPriceAtAddition()) + "</td>");
                out.println("<td>" + formatPrice(item.getLineTotal()) + "</td>");
                out.println("</tr>");
            }
            out.println("</table>");
            out.println("<p class='total'>Total: " + formatPrice(currentOrder.getCalculatedTotalAmount()) + "</p>");
            out.println("</div>");
        } else {
            out.println("<p>No active order yet. Add an item to start.</p>");
        }

        // -------------------- FORM --------------------
        out.println("<form method='post' action='shop' class='shop-form'>");
        out.println("<h3>Manage Order</h3>");

        out.println("<label for='itemCode'>Select Item:</label>");
        out.println("<select name='itemCode' required>");
        if (websiteStocks != null) {
            for (WebsiteStock stock : websiteStocks) {
                Item s = stock.getItem();
                out.println("<option value='" + s.getItemCode() + "'>"
                        + s.getName() + " - " + formatPrice(s.getUnitPrice())
                        + " (" + stock.getQuantityAvailableOnline() + " available)</option>");
            }
        }
        out.println("</select>");

        out.println("<label for='quantity'>Quantity:</label>");
        out.println("<input type='number' name='quantity' min='1' value='1' required>");

        out.println("<div class='form-buttons'>");
        out.println("<button type='submit' name='action' value='add'>Add</button>");
        out.println("<button type='submit' name='action' value='update'>Update</button>");
        out.println("<button type='submit' name='action' value='remove'>Remove</button>");
        out.println("</div>");

        out.println("<h3>Checkout</h3>");
        out.println("<label for='shippingAddress'>Shipping Address:</label>");
        out.println("<input type='text' name='shippingAddress' placeholder='Enter shipping address'>");
        out.println("<button type='submit' name='action' value='checkout' class='checkout-btn'>Checkout</button>");

        out.println("</form>");
        out.println("<p><a href='home'>Back to Main Menu</a></p>");
        out.println("</div>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        AuthenticatedUser user = (AuthenticatedUser) req.getSession().getAttribute("user");
        if (user == null || user.getRole() != com.syos.domain.enums.UserRole.CUSTOMER) {
            out.println("<p>Please <a href='login'>login as a customer</a>.</p>");
            return;
        }

        String action = req.getParameter("action");
        String itemCode = req.getParameter("itemCode");
        String qtyParam = req.getParameter("quantity");
        String shippingAddress = req.getParameter("shippingAddress");

        int quantity = 0;
        if (qtyParam != null && !qtyParam.isEmpty()) {
            try {
                quantity = Integer.parseInt(qtyParam);
            } catch (NumberFormatException e) {
                out.println("<p>Invalid quantity.</p>");
                return;
            }
        }

        try {
            switch (action) {
                case "add" -> {
                    onlineOrderingService.addItemToActiveOrder(user.getId(), itemCode, quantity);
                    out.println("<p>✅ Item added successfully!</p>");
                }
                case "update" -> {
                    onlineOrderingService.updateOrderItemQuantity(user.getId(), itemCode, quantity);
                    out.println("<p>✅ Quantity updated successfully!</p>");
                }
                case "remove" -> {
                    onlineOrderingService.removeItemFromActiveOrder(user.getId(), itemCode);
                    out.println("<p>✅ Item removed successfully!</p>");
                }
                case "checkout" -> {
                    Bill bill = onlineOrderingService.checkoutOrder(user.getId(), shippingAddress);
                    out.println("<p>✅ Checkout successful!</p>");
                    out.println("<pre>" + bill.generateReceipt() + "</pre>");
                }
                default -> out.println("<p>❌ Unknown action.</p>");
            }
        } catch (ItemNotFoundException | InsufficientStockException | DatabaseOperationException |
                 OrderProcessingException | IllegalArgumentException | IllegalStateException e) {
            out.println("<p>Error: " + e.getMessage() + "</p>");
        }

        out.println("<p><a href='shop'>Back to Shop Menu</a></p>");
        out.println("<p><a href='home'>Back to Main Menu</a></p>");
    }

    void setOnlineOrderingService(OnlineOrderingService svc) {
        this.onlineOrderingService = svc;
    }
    void setStockManagementService(StockManagementService svc) {
        this.stockManagementService = svc;
    }
    void setOnlineUserService(OnlineUserService svc) {
        this.onlineUserService = svc;
    }


    private String formatPrice(BigDecimal price) {
        return price != null ? String.format("$%.2f", price) : "$0.00";
    }
}
