<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Stock Management</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/stock.css"/>
</head>
<body>

<h1>🧾 Stock Management</h1>

<!-- Navigation -->
<nav>
    <a href="${pageContext.request.contextPath}/stockAction?action=batchList">📦 View Stock Batches</a>
    <a href="${pageContext.request.contextPath}/stockAction?action=shelfStock">🪑 View Shelf Stock</a>
    <a href="${pageContext.request.contextPath}/stockAction?action=websiteStock">🌐 View Website Stock</a>
    <a href="${pageContext.request.contextPath}/stockAction?action=removeExpired">🧼 Remove Expired Stock</a>
</nav>

<!-- Messages -->
<c:if test="${not empty sessionScope.stockActionMessage}">
    <div class="message">${sessionScope.stockActionMessage}</div>
    <c:remove var="stockActionMessage" scope="session"/>
</c:if>

<c:if test="${not empty sessionScope.stockActionError}">
    <div class="error">${sessionScope.stockActionError}</div>
    <c:remove var="stockActionError" scope="session"/>
</c:if>

<!-- All Items Table -->
<h2>📋 All Items</h2>
<table>
    <thead>
        <tr>
            <th>Item Code</th>
            <th>Name</th>
            <th>Description</th>
            <th>Category</th>
            <th>Unit Price</th>
            <th>Reorder Level</th>
            <th>Reorder Quantity</th>
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
            <td>
                <!-- Delete button -->
                <form action="${pageContext.request.contextPath}/stockAction" method="post" class="inline">
                    <input type="hidden" name="action" value="deleteItem"/>
                    <input type="hidden" name="itemCode" value="${item.itemCode}"/>
                    <button type="submit" class="delete">🗑️ Delete</button>
                </form>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<!-- Add Item Form -->
<h2>➕ Add New Item</h2>
<form action="${pageContext.request.contextPath}/stockAction" method="post" class="add-form">
    <input type="hidden" name="action" value="addItem"/>
    <input type="text" name="itemCode" placeholder="Item Code" required/>
    <input type="text" name="name" placeholder="Name" required/>
    <input type="text" name="description" placeholder="Description" required/>
    <input type="text" name="category" placeholder="Category" required/>
    <input type="number" step="0.01" name="unitPrice" placeholder="Unit Price" required/>
    <input type="number" name="reorderLevel" placeholder="Reorder Level" required/>
    <input type="number" name="reorderQuantity" placeholder="Reorder Quantity" required/>
    <button type="submit">✅ Add Item</button>
</form>

</body>
</html>
