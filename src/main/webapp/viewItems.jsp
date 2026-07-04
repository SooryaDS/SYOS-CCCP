<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Inventory Items</title>
    <link rel="stylesheet" href="css/stock.css">
</head>
<body>
    <h1>Inventory Items</h1>

    <!-- Flash Messages -->
    <%
        String successMsg = (String) session.getAttribute("successMessage");
        String errorMsg = (String) session.getAttribute("errorMessage");

        if (successMsg != null) {
    %>
        <div class="alert success"><%= successMsg %></div>
    <%
            session.removeAttribute("successMessage");
        }
        if (errorMsg != null) {
    %>
        <div class="alert error"><%= errorMsg %></div>
    <%
            session.removeAttribute("errorMessage");
        }
    %>

    <!-- Add New Item Form -->
    <form action="stockAction" method="post" class="add-item-form">
        <input type="hidden" name="action" value="addItem">
        <input type="text" name="itemCode" placeholder="Item Code" required>
        <input type="text" name="name" placeholder="Item Name" required>
        <input type="text" name="description" placeholder="Description">
        <input type="text" name="category" placeholder="Category">
        <input type="number" step="0.01" name="unitPrice" placeholder="Unit Price" required>
        <input type="number" name="reorderLevel" placeholder="Reorder Level" required>
        <input type="number" name="reorderQuantity" placeholder="Reorder Quantity" required>
        <button type="submit">Add Item</button>
    </form>

    <!-- Items Table -->
    <table>
        <thead>
            <tr>
                <th>Item Code</th>
                <th>Name</th>
                <th>Description</th>
                <th>Category</th>
                <th>Unit Price</th>
                <th>Reorder Level</th>
                <th>Reorder Qty</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="item" items="${items}">
                <tr>
                    <td>${item.itemCode}</td>
                    <td>${item.name}</td>
                    <td>${item.description}</td>
                    <td>${item.category}</td>
                    <td>${item.unitPrice}</td>
                    <td>${item.reorderLevel}</td>
                    <td>${item.reorderQuantity}</td>
                    <td class="actions">
                        <form action="stockAction" method="post" style="display:inline;">
                            <input type="hidden" name="action" value="deleteItem">
                            <input type="hidden" name="itemCode" value="${item.itemCode}">
                            <button type="submit" style="background-color: #dc3545;">Delete</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>

            <c:if test="${empty items}">
                <tr>
                    <td colspan="8" class="no-data">No items found.</td>
                </tr>
            </c:if>
        </tbody>
    </table>

    <br>
    <div class="nav-links">
        <a href="stockAction?action=viewBatches">View Stock Batches</a>
        <a href="stockAction?action=viewShelf">View Shelf Stock</a>
        <a href="stockAction?action=viewWebsite">View Website Stock</a>
        <a href="stockAction?action=removeExpired">Remove Expired Stock</a>
    </div>

    <br>
    <div class="nav-links">
        <a href="${pageContext.request.contextPath}/employeeMenu" style="font-weight:bold;">⬅ Back to Employee Menu</a>
    </div>
</body>
</html>
