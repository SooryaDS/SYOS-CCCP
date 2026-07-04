<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isErrorPage="true" %>
<html>
<head>
    <title>Error - Stock Management</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f8d7da;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }
        .error-box {
            background: #fff;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            border-left: 5px solid #dc3545;
            max-width: 600px;
            width: 90%;
        }
        h1 {
            color: #dc3545;
            margin-top: 0;
        }
        p {
            color: #333;
        }
        pre {
            background-color: #f1f1f1;
            padding: 10px;
            overflow-x: auto;
            max-height: 200px;
        }
        a {
            display: inline-block;
            margin-top: 15px;
            color: #007bff;
            text-decoration: none;
        }
    </style>
</head>
<body>
<div class="error-box">
    <h1>⚠️ Something went wrong</h1>
    <p><strong>Error Message:</strong> ${errorMessage}</p>
    <c:if test="${not empty exception}">
        <p><strong>Details:</strong></p>
        <pre><%= exception %></pre>
    </c:if>
    <a href="stockAction?action=viewItems">⬅ Back to Items</a>
</div>
</body>
</html>
