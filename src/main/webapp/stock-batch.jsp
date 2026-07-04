<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.syos.domain.model.StockBatch" %>
<html>
<head>
    <title>Stock Management</title>
    <link rel="stylesheet" href="css/stock.css">
    <script>
        function toggleForms() {
            const action = document.getElementById("stockActionSelect").value;
            document.getElementById("addStockForm").style.display = action === "add" ? "block" : "none";
            document.getElementById("removeStockForm").style.display = action === "remove" ? "block" : "none";
        }
    </script>
</head>
<body>
<h1>Stock Management</h1>

<!-- Flash Messages -->
<%
    String msg = (String) session.getAttribute("stockActionMessage");
    String err = (String) session.getAttribute("stockActionError");
    if (msg != null) {
%>
    <div class="alert success"><%= msg %></div>
<%
        session.removeAttribute("stockActionMessage");
    }
    if (err != null) {
%>
    <div class="alert error"><%= err %></div>
<%
        session.removeAttribute("stockActionError");
    }
%>

<!-- Action Selector -->
<div class="form-group">
    <label for="stockActionSelect">Choose Action:</label>
    <select id="stockActionSelect" onchange="toggleForms()">
        <option value="add">Add Stock</option>
        <option value="remove">Remove Stock</option>
    </select>
</div>

<!-- Forms Container -->
<div class="stock-forms-container">

    <!-- Add Stock Form -->
    <form id="addStockForm" action="stockAction" method="post" class="stock-form">
        <input type="hidden" name="action" value="receiveBatch">

        <label>Item Code:</label>
        <input type="text" name="itemCode" placeholder="Enter Item Code" required>

        <label>Quantity:</label>
        <input type="number" name="quantity" required>

        <label>Cost per Unit:</label>
        <input type="number" step="0.01" name="costPerUnit" required>

        <label>Expiry Date:</label>
        <input type="date" name="expiryDate">

        <button type="submit">Add Stock</button>
    </form>

    <!-- Remove Stock Form -->
    <form id="removeStockForm" action="stockAction" method="post" class="stock-form" style="display: none;">
        <input type="hidden" name="action" value="deleteStockBatch">

        <label>Select Batch to Delete:</label>
        <select name="batchId" required>
            <option value="">-- Select Batch --</option>
            <%
                List<StockBatch> stockBatches = (List<StockBatch>) request.getAttribute("stockBatches");
                if (stockBatches != null) {
                    for (StockBatch batch : stockBatches) {
            %>
            <option value="<%= batch.getBatchId() %>">
                ID: <%= batch.getBatchId() %> | Item: <%= batch.getItem() != null ? batch.getItem().getItemCode() : "N/A" %> | Qty: <%= batch.getCurrentQuantityInStore() %>
            </option>
            <%
                    }
                }
            %>
        </select>

        <button type="submit">Delete Selected Batch</button>
    </form>
</div>

<!-- Stock Table -->
<div class="stock-table-container">
    <h2>Current Stock Batches</h2>
    <table>
        <thead>
        <tr>
            <th>Batch ID</th>
            <th>Item Code</th>
            <th>Received Qty</th>
            <th>Current Qty</th>
            <th>Cost per Unit</th>
            <th>Expiry Date</th>
            <th>Received Date</th>
        </tr>
        </thead>
        <tbody>
        <%
            if (stockBatches != null && !stockBatches.isEmpty()) {
                for (StockBatch batch : stockBatches) {
        %>
        <tr>
            <td><%= batch.getBatchId() %></td>
            <td><%= batch.getItem() != null ? batch.getItem().getItemCode() : "N/A" %></td>
            <td><%= batch.getReceivedQuantity() %></td>
            <td><%= batch.getCurrentQuantityInStore() %></td>
            <td><%= batch.getCostPerUnit() %></td>
            <td><%= batch.getExpiryDate() != null ? batch.getExpiryDate() : "—" %></td>
            <td><%= batch.getReceivedDate() %></td>
        </tr>
        <%
                }
            } else {
        %>
        <tr>
            <td colspan="7" class="no-data">No stock batches found.</td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>

<!-- Navigation -->
<br>
<div class="nav-links">
    <a href="stockAction?action=viewItems">View Items</a>
    <a href="stockAction?action=viewShelf">View Shelf Stock</a>
    <a href="stockAction?action=viewWebsite">View Website Stock</a>
    <a href="stockAction?action=removeExpired">Remove Expired Stock</a>
</div>

<script>
    toggleForms();
</script>
</body>
</html>
