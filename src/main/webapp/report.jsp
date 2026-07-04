<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*, java.math.BigDecimal" %>
<%@ page import="com.syos.adapter.dto.*" %>
<!DOCTYPE html>
<html>
<head>
    <title>Reporting Dashboard</title>
    <link rel="stylesheet" href="css/report.css">
</head>
<body>

<h1>Reporting Dashboard</h1>

<% String error = (String) request.getAttribute("error");
   if (error != null) { %>
   <div class="error"><%= error %></div>
<% } %>

<!-- Daily Sales Report -->
<form action="report" method="post">
    <input type="hidden" name="action" value="dailySales">
    <h2>Daily Sales Report</h2>
    <label>Date:</label> <input type="date" name="date" required>
    <label>Type:</label>
    <select name="transactionType">
        <option value="">All</option>
        <option value="POS">POS</option>
        <option value="ONLINE">Online</option>
    </select>
    <button type="submit">Generate</button>
</form>

<!-- Reorder Level Report -->
<form action="report" method="post">
    <input type="hidden" name="action" value="reorder">
    <h2>Reorder Level Report</h2>
    <button type="submit">Generate</button>
</form>

<!-- Batch Stock Report -->
<form action="report" method="post">
    <input type="hidden" name="action" value="batchStock">
    <h2>Batch Stock Report</h2>
    <button type="submit">Generate</button>
</form>

<!-- Reshelving Report -->
<form action="report" method="post">
    <input type="hidden" name="action" value="reshelving">
    <h2>Daily Reshelving Needs</h2>
    <button type="submit">Generate</button>
</form>

<!-- Bill Transaction Report -->
<form action="report" method="post">
    <input type="hidden" name="action" value="billTransactions">
    <h2>Bill Transaction Report</h2>
    <label>Start:</label> <input type="date" name="startDate">
    <label>End:</label> <input type="date" name="endDate">
    <label>Type:</label>
    <select name="transactionType">
        <option value="">All</option>
        <option value="POS">POS</option>
        <option value="ONLINE">Online</option>
    </select>
    <button type="submit">Generate</button>
</form>

<hr>

<%
    Object dailySalesReportObj = request.getAttribute("dailySalesReport");
    Object reorderReportObj = request.getAttribute("reorderReport");
    Object batchStockReportObj = request.getAttribute("batchStockReport");
    Object reshelvingReportObj = request.getAttribute("reshelvingReport");
    Object billReportObj = request.getAttribute("billReport");
%>

<!-- ================= DAILY SALES REPORT ================= -->
<% if (dailySalesReportObj != null && dailySalesReportObj instanceof DailySalesReportDTO) {
       DailySalesReportDTO report = (DailySalesReportDTO) dailySalesReportObj;
       List<DailySalesReportItemDTO> items = report.getSaleItems();
%>
    <h2>Daily Sales Report Results</h2>
    <p><strong>Total Revenue:</strong> <%= report.getOverallTotalRevenue() %></p>
    <table>
        <thead>
            <tr>
                <th>Item Code</th>
                <th>Item Name</th>
                <th>Quantity Sold</th>
                <th>Total Revenue</th>
            </tr>
        </thead>
        <tbody>
        <% if (items != null && !items.isEmpty()) {
               for (DailySalesReportItemDTO item : items) { %>
            <tr>
                <td><%= item.getItemCode() %></td>
                <td><%= item.getItemName() %></td>
                <td><%= item.getTotalQuantitySold() %></td>
                <td><%= item.getTotalRevenueForItem() %></td>
            </tr>
        <% } } else { %>
            <tr><td colspan="4">No data found.</td></tr>
        <% } %>
        </tbody>
    </table>
<% } %>

<!-- ================= REORDER REPORT ================= -->
<% if (reorderReportObj != null && reorderReportObj instanceof List) {
       List<ReorderReportItemDTO> reorderItems = (List<ReorderReportItemDTO>) reorderReportObj;
%>
    <h2>Reorder Level Report</h2>
    <table>
        <thead>
            <tr>
                <th>Item Code</th>
                <th>Item Name</th>
                <th>Current Total Stock</th>
                <th>Reorder Threshold</th>
            </tr>
        </thead>
        <tbody>
        <% if (!reorderItems.isEmpty()) {
               for (ReorderReportItemDTO item : reorderItems) { %>
            <tr>
                <td><%= item.getItemCode() %></td>
                <td><%= item.getItemName() %></td>
                <td><%= item.getCurrentTotalStock() %></td>
                <td><%= item.getReorderLevelThreshold() %></td>
            </tr>
        <% } } else { %>
            <tr><td colspan="4">No data found.</td></tr>
        <% } %>
        </tbody>
    </table>
<% } %>

<!-- ================= RESHELVING REPORT ================= -->
<% if (reshelvingReportObj != null && reshelvingReportObj instanceof List) {
       List<ReshelvingNeedsItemDTO> reshelvingItems = (List<ReshelvingNeedsItemDTO>) reshelvingReportObj;
%>
    <h2>Daily Reshelving Needs</h2>
    <table>
        <thead>
            <tr>
                <th>Item Code</th>
                <th>Item Name</th>
                <th>Current Shelf Quantity</th>
                <th>Min Threshold</th>
                <th>Quantity to Reshelve</th>
            </tr>
        </thead>
        <tbody>
        <% if (!reshelvingItems.isEmpty()) {
               for (ReshelvingNeedsItemDTO item : reshelvingItems) { %>
            <tr>
                <td><%= item.getItemCode() %></td>
                <td><%= item.getItemName() %></td>
                <td><%= item.getCurrentShelfQuantity() %></td>
                <td><%= item.getMinShelfStockThreshold() %></td>
                <td><%= item.getQuantityToReshelve() %></td>
            </tr>
        <% } } else { %>
            <tr><td colspan="5">No data found.</td></tr>
        <% } %>
        </tbody>
    </table>
<% } %>

<!-- ================= BILL REPORT ================= -->
<% if (billReportObj != null && billReportObj instanceof List) {
       List<Map<String,Object>> rows = (List<Map<String,Object>>) billReportObj;
%>
    <h2>Bill Transaction Report</h2>
    <% if (!rows.isEmpty()) {
           Set<String> headers = rows.get(0).keySet();
    %>
    <table>
        <thead>
            <tr>
                <% for (String header : headers) { %>
                    <th><%= header %></th>
                <% } %>
            </tr>
        </thead>
        <tbody>
            <% for (Map<String,Object> row : rows) { %>
                <tr>
                    <% for (String header : headers) { %>
                        <td><%= row.get(header) %></td>
                    <% } %>
                </tr>
            <% } %>
        </tbody>
    </table>
    <% } else { %>
        <p>No bill transactions found.</p>
    <% } %>
<% } %>

</body>
</html>
