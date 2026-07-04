<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Website Stock</title>
    <link rel="stylesheet" href="css/stock.css">
</head>
<body>
    <h1>Website Stock</h1>

    <!-- Add or Remove Website Stock -->
    <form action="stockAction" method="post" onsubmit="return setActionBasedOnOperation(this)">
        <input type="hidden" name="action" value="">
        <select name="operation" required>
            <option value="">-- Select Operation --</option>
            <option value="add">Add to Website</option>
            <option value="remove">Remove from Website</option>
        </select>

        <select name="itemCode" required>
            <option value="">-- Select Item --</option>
            <c:forEach var="item" items="${items}">
                <option value="${item.itemCode}">${item.itemCode} - ${item.name}</option>
            </c:forEach>
        </select>

        <input type="number" name="quantity" placeholder="Quantity" min="1" required>
        <button type="submit">Confirm</button>
    </form>

    <!-- Website Stock Table -->
    <table>
        <tr>
            <th>Website Stock ID</th>
            <th>Item Code</th>
            <th>Item Name</th>
            <th>Quantity Online</th>
            <th>Last Updated</th>
            <th>Actions</th>
        </tr>
        <c:forEach var="ws" items="${websiteStock}">
            <tr>
                <td>${ws.websiteStockId}</td>
                <td>${ws.item.itemCode}</td>
                <td>${ws.item.name}</td>
                <td>${ws.quantityAvailableOnline}</td>
                <td>${ws.lastUpdatedDate}</td>
                <td class="actions">
                    <form action="stockAction" method="post" style="display:inline;">
                        <input type="hidden" name="action" value="removeWebsite">
                        <input type="hidden" name="itemCode" value="${ws.item.itemCode}">
                        <input type="hidden" name="quantity" value="${ws.quantityAvailableOnline}">
                        <button type="submit" style="background-color:#dc3545;">Delete</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty websiteStock}">
            <tr><td colspan="6" class="no-data">No website stock records found.</td></tr>
        </c:if>
    </table>

    <br>
    <a href="stockAction?action=viewItems">Back to Items</a>

    <script>
        function setActionBasedOnOperation(form) {
            const op = form.operation.value;
            if (op === 'add') {
                form.action.value = 'addWebsite';
            } else if (op === 'remove') {
                form.action.value = 'removeWebsite';
            } else {
                alert("Please select an operation");
                return false;
            }
            return true;
        }
    </script>
</body>
</html>
