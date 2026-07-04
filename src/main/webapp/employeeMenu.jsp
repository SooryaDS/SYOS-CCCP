<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Employee Menu</title>

    <link rel="stylesheet" href="css/employeeMenu.css?">
</head>

<body>

    <!-- Logout Button -->
    <a class="logout-button" href="logout" title="Logout">
        <img src="images/logout.png" alt="Logout">
    </a>

    <div class="container">
        <h2>Welcome, ${username} (Staff)</h2>

        <div class="button-row">
            <a class="menu-button" href="counter">
                <img src="images/POS.png" alt="POS">
                POS / Counter Sales
            </a>

            <a class="menu-button" href="stockAction">
                <img src="images/stock.png" alt="Stock">
                Stock Management
            </a>

            <a class="menu-button" href="report">
                <img src="images/report.png" alt="Report">
                Reporting
            </a>
        </div>
    </div>

</body>
</html>
