<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.syos.domain.model.StockBatch" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>

<%
    List<StockBatch> expiredRemoved = (List<StockBatch>) request.getAttribute("expiredRemoved");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Expired Stock Removal</title>
    <style>
        body { font-family: Arial, sans-serif; background: #f7f7f7; margin: 20px; }
        h1 { color: #c00; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #999; padding: 8px; text-align: left; }
        th { background-color: #eee; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        .no-data { color: #555; margin-top: 20px; }
        a { text-decoration: none; color: #007BFF; }
    </style>
</head>
<body>
<h1>Expired Stock Removal</h1>

<% if (expiredRemoved == null || expiredRemoved.isEmpty()) { %>
    <p class="no-data">No expired stock batches were found or removed.</p>
<% } else { %>
    <p>Removed <strong><%= expiredRemoved.size() %></strong> expired stock batch(es):</p>
    <table>
        <tr>
            <th>Batch ID</th>
            <th>Item Code</th>
            <th>Item Name</th>
            <th>Expiry Date</th>
            <th>Quantity in Batch</th>
        </tr>
        <% for (StockBatch batch : expiredRemoved) { %>
            <tr>
                <td><%= batch.getBatchId() %></td>
                <td><%= batch.getItem().getItemCode() %></td>
                <td><%= batch.getItem().getName() %></td>
                <td><%= batch.getExpiryDate() != null ? batch.getExpiryDate().format(formatter) : "N/A" %></td>
                <td><%= batch.getCurrentQuantityInStore() %></td>
            </tr>
        <% } %>
    </table>
<% } %>

<p><a href="stockAction?action=viewBatches">Back to Stock Batches</a> |
   <a href="stockAction?action=viewItems">Back to Items</a></p>

</body>
</html>
