<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.syos.domain.model.Bill, com.syos.domain.model.BillItem" %>
<%@ page import="java.util.List" %>

<%
    Bill currentBill = (Bill) session.getAttribute("currentBill");
    Bill finalizedBill = (Bill) session.getAttribute("finalizedBill");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Counter Sales (POS)</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/counterSales.css">
</head>
<body>
    <h2>Counter Sales (POS)</h2>

    <% if (error != null) { %>
        <p class="error"><%= error %></p>
    <% } %>
    <% if (success != null) { %>
        <p class="success"><%= success %></p>
    <% } %>

    <form method="post">
        <input type="submit" name="action" value="Start New Bill"><br><br>

        Item Code: <input name="itemCode"><br>
        Quantity: <input name="quantity" type="number" min="1"><br>
        <input type="submit" name="action" value="Add Item"><br><br>

        <input type="submit" name="action" value="Finalize Bill"><br><br>

        <a href="${pageContext.request.contextPath}/home">Back to Main Menu</a>
    </form>

    <%-- 🧾 Current Bill Section --%>
    <%
        if (currentBill != null && currentBill.getBillItems() != null && !currentBill.getBillItems().isEmpty()) {
            List<BillItem> billItems = currentBill.getBillItems();
    %>
        <h3>Current Bill Items</h3>
        <table>
            <thead>
                <tr>
                    <th>Item Code</th>
                    <th>Item Name</th>
                    <th>Quantity</th>
                    <th>Price Per Unit</th>
                    <th>Total</th>
                </tr>
            </thead>
            <tbody>
            <% for (BillItem bi : billItems) { %>
                <tr>
                    <td><%= bi.getItem().getItemCode() %></td>
                    <td><%= bi.getItemNameAtSale() %></td>
                    <td><%= bi.getQuantityPurchased() %></td>
                    <td>Rs. <%= bi.getPricePerUnitAtSale() %></td>
                    <td>Rs. <%= bi.getTotalPriceForItem() %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
        <h4>Subtotal: Rs. <%= currentBill.getSubTotalAmount() %></h4>
        <h4>Total: Rs. <%= currentBill.getFinalTotalAmount() %></h4>
    <% } %>

    <%-- ✅ Finalized Bill Section --%>
    <%
        if (finalizedBill != null && finalizedBill.getBillItems() != null && !finalizedBill.getBillItems().isEmpty()) {
            List<BillItem> finalItems = finalizedBill.getBillItems();
    %>
        <hr>
        <h2>✅ Finalized Bill</h2>
        <table>
            <thead>
                <tr>
                    <th>Item Code</th>
                    <th>Item Name</th>
                    <th>Quantity</th>
                    <th>Price Per Unit</th>
                    <th>Total</th>
                </tr>
            </thead>
            <tbody>
            <% for (BillItem bi : finalItems) { %>
                <tr>
                    <td><%= bi.getItem().getItemCode() %></td>
                    <td><%= bi.getItemNameAtSale() %></td>
                    <td><%= bi.getQuantityPurchased() %></td>
                    <td>Rs. <%= bi.getPricePerUnitAtSale() %></td>
                    <td>Rs. <%= bi.getTotalPriceForItem() %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
        <h4>Subtotal: Rs. <%= finalizedBill.getSubTotalAmount() %></h4>
        <h3>Total: Rs. <%= finalizedBill.getFinalTotalAmount() %></h3>
    <% } %>
</body>
</html>
