<html>
<head>
    <title>Online Shop</title>
    <link rel="stylesheet" href="css/shop.css">
</head>
<body>
<div class="container">
    <h2>Welcome, ${username}!</h2>
    <div class="message success">${msg}</div>
    <div class="message error">${error}</div>

    <form method="post" action="shop">
        <select name="itemCode" required>
            <option value="">-- Select Item --</option>
            <!-- Dynamically generate this with website stock items -->
            <c:forEach var="item" items="${websiteStock}">
                <option value="${item.itemCode}">
                    ${item.item.name} (Available: ${item.quantity})
                </option>
            </c:forEach>
        </select>

        <input type="number" name="quantity" min="1" placeholder="Quantity" required>

        <button type="submit" name="action" value="add">Add</button>
        <button type="submit" name="action" value="update">Update</button>
        <button type="submit" name="action" value="remove">Remove</button>
    </form>

    <!-- Cart Table -->
    <c:if test="${not empty currentOrderItems}">
        <table>
            <thead>
            <tr>
                <th>Item Code</th>
                <th>Name</th>
                <th>Qty</th>
                <th>Price</th>
                <th>Total</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="orderItem" items="${currentOrderItems}">
                <tr>
                    <td>${orderItem.itemCode}</td>
                    <td>${orderItem.item.name}</td>
                    <td>${orderItem.quantity}</td>
                    <td>${orderItem.item.unitPrice}</td>
                    <td>${orderItem.totalPrice}</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    <!-- Checkout -->
    <form method="post" action="shop">
        <input type="text" name="shippingAddress" placeholder="Shipping Address" required>
        <button type="submit" name="action" value="checkout">Checkout</button>
    </form>

    <a href="home">⬅ Back to Main Menu</a>
</div>
</body>
</html>
