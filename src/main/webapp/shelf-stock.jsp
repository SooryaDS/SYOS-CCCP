<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Shelf Stock</title>
    <link rel="stylesheet" href="css/stock.css">
</head>
<body>
    <h1>Shelf Stock</h1>

    <!-- Add or Remove Shelf Stock -->
    <form action="stockAction" method="post" onsubmit="return setShelfAction(this)">
        <input type="hidden" name="action" value="">
        <select name="operation" required>
            <option value="">-- Select Operation --</option>
            <option value="add">Add to Shelf</option>
            <option value="remove">Remove from Shelf</option>
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

    <!-- Shelf Stock Table -->
    <table>
        <tr>
            <th>Shelf Stock ID</th>
            <th>Item Code</th>
            <th>Item Name</th>
            <th>Quantity on Shelf</th>
            <th>Last Updated</th>
            <th>Actions</th>
        </tr>
        <c:forEach var="ss" items="${shelfStock}">
            <tr>
                <td>${ss.shelfStockId}</td>
                <td>${ss.item.itemCode}</td>
                <td>${ss.item.name}</td>
                <td>${ss.quantityOnShelf}</td>
                <td>${ss.lastUpdatedDate}</td>
                <td class="actions">

                    <form action="stockAction" method="post" style="display:inline;">
                        <input type="hidden" name="action" value="removeShelf">
                        <input type="hidden" name="itemCode" value="${ss.item.itemCode}">
                        <input type="hidden" name="quantity" value="${ss.quantityOnShelf}">
                        <button type="submit" style="background-color:#dc3545;"
                                onclick="return confirm('Are you sure you want to delete this shelf stock?');">
                            Delete
                        </button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty shelfStock}">
            <tr><td colspan="7" class="no-data">No shelf stock records found.</td></tr>
        </c:if>
    </table>

    <br>
    <a href="stockAction?action=viewItems">Back to Items</a>

    <script>
        function setShelfAction(form) {
            const op = form.operation.value;
            if (op === 'add') {
                form.action.value = 'addShelf';
            } else if (op === 'remove') {
                form.action.value = 'removeShelf';
            } else {
                alert("Please select an operation");
                return false;
            }
            return true;
        }
    </script>
</body>
</html>
