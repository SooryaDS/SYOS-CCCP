package com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.domain.model.Bill;
import com.syos.domain.model.BillItem;
import com.syos.domain.model.Item;
import com.syos.domain.enums.TransactionType;
import com.syos.adapter.dto.BillSummaryDTO;
import com.syos.adapter.dto.DailySalesReportItemDTO; // Added this import, assuming you'll use it
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;
import com.syos.service.payment.PaymentResult; // Import PaymentResult

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal; // Ensure BigDecimal is imported
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BillRepositoryImpl implements BillRepository {

    private final ItemRepository itemRepository;

    public BillRepositoryImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public String findNextBillSerialNumberForToday() throws DatabaseOperationException {
        LocalDate today = LocalDate.now();
        String datePrefix = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String maxBillSerialSql = "SELECT bill_serial_number FROM bills WHERE bill_serial_number LIKE ? ORDER BY bill_serial_number DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(maxBillSerialSql)) {

            pstmt.setString(1, datePrefix + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                int sequence = 1;
                if (rs.next()) {
                    String lastBillSerial = rs.getString("bill_serial_number");
                    if (lastBillSerial != null && lastBillSerial.length() >= 8) {
                        try {
                            sequence = Integer.parseInt(lastBillSerial.substring(8)) + 1;
                        } catch (NumberFormatException e) {
                            System.err.println("Warning: Could not parse sequence from bill serial: " + lastBillSerial + ". Starting sequence from 1.");
                            sequence = 1;
                        }
                    }
                }
                return datePrefix + String.format("%04d", sequence);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching next bill serial number.", e);
        }
    }

    @Override
    public Bill save(Bill bill) throws DatabaseOperationException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            Bill savedBill = save(bill, conn);
            conn.commit();
            return savedBill;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* Log rollback error */ }
            }
            if(e instanceof DatabaseOperationException) throw (DatabaseOperationException) e;
            throw new DatabaseOperationException("Error saving bill.", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* Log close error */ }
            }
        }
    }

    @Override
    public Bill save(Bill bill, Connection conn) throws DatabaseOperationException {
        String billSql = "INSERT INTO bills (bill_serial_number, bill_date, transaction_type, user_id, employee_id, sub_total_amount, discount_amount, final_total_amount, cash_tendered, change_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String billItemSql = "INSERT INTO bill_items (bill_id, bill_serial_number, bill_date, item_code, item_name_at_sale, quantity_purchased, price_per_unit_at_sale, total_price_for_item) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement billPstmt = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement billItemPstmt = conn.prepareStatement(billItemSql)) {

            // Set values for the 'bills' table
            billPstmt.setString(1, bill.getBillSerialNumber());
            billPstmt.setTimestamp(2, Timestamp.valueOf(bill.getBillDate()));
            billPstmt.setString(3, bill.getTransactionType().name());

            // User ID: Use setInt for Integer, handle null
            if (bill.getUserId() != null) {
                billPstmt.setInt(4, bill.getUserId());
            } else {
                billPstmt.setNull(4, java.sql.Types.INTEGER);
            }

            // Employee ID: Still String in Bill model, use setString, handle null
            if (bill.getEmployeeId() != null) {
                billPstmt.setString(5, bill.getEmployeeId());
            } else {
                billPstmt.setNull(5, java.sql.Types.VARCHAR);
            }

            billPstmt.setBigDecimal(6, bill.getSubTotalAmount());
            billPstmt.setBigDecimal(7, bill.getDiscountAmount());
            billPstmt.setBigDecimal(8, bill.getFinalTotalAmount());

            // Extract cashTendered and changeAmount from PaymentResult if present, otherwise null
            BigDecimal cashTendered = (bill.getPaymentResult() != null) ? bill.getPaymentResult().getAmountTendered() : null;
            BigDecimal changeAmount = (bill.getPaymentResult() != null) ? bill.getPaymentResult().getChangeGiven() : null;

            if (cashTendered != null) {
                billPstmt.setBigDecimal(9, cashTendered);
            } else {
                billPstmt.setNull(9, java.sql.Types.DECIMAL);
            }
            if (changeAmount != null) {
                billPstmt.setBigDecimal(10, changeAmount);
            } else {
                billPstmt.setNull(10, java.sql.Types.DECIMAL);
            }


            int affectedRows = billPstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating bill failed, no rows affected.");
            }

            try (ResultSet generatedKeys = billPstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bill.setId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseOperationException("Creating bill failed, no ID obtained.");
                }
            }

            // Save Bill Items
            for (BillItem bi : bill.getBillItems()) {
                bi.setBillId(bill.getId());
                bi.setBillSerialNumber(bill.getBillSerialNumber());

                billItemPstmt.setInt(1, bill.getId());
                billItemPstmt.setString(2, bi.getBillSerialNumber());
                billItemPstmt.setTimestamp(3, Timestamp.valueOf(bill.getBillDate()));
                billItemPstmt.setString(4, bi.getItem().getItemCode());
                billItemPstmt.setString(5, bi.getItemNameAtSale());
                billItemPstmt.setInt(6, bi.getQuantityPurchased());
                billItemPstmt.setBigDecimal(7, bi.getPricePerUnitAtSale());
                billItemPstmt.setBigDecimal(8, bi.getTotalPriceForItem());
                billItemPstmt.addBatch();
            }
            billItemPstmt.executeBatch();
            return bill;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving bill within transaction", e);
        }
    }

    /**
     * Maps a ResultSet row to a Bill object.
     * @param rs The ResultSet containing bill data.
     * @return A Bill object.
     * @throws SQLException If a SQL error occurs.
     */
    private Bill mapRowToBill(ResultSet rs) throws SQLException, DatabaseOperationException {
        // userId can be null in the database, use getObject with Integer.class
        Integer userId = rs.getObject("user_id", Integer.class);
        String employeeId = rs.getString("employee_id");

        // Reconstruct PaymentResult from DB columns
        BigDecimal cashTendered = rs.getBigDecimal("cash_tendered");
        BigDecimal changeAmount = rs.getBigDecimal("change_amount");
        PaymentResult paymentResult = null;
        if (cashTendered != null) {
            paymentResult = new PaymentResult(true, cashTendered, changeAmount, "Payment retrieved from DB.");
        }

        // Assuming Bill constructor also needs item details if you were using it.
        // For simplicity, directly mapping here.
        return new Bill(
                rs.getInt("id"),
                rs.getString("bill_serial_number"),
                rs.getTimestamp("bill_date").toLocalDateTime(),
                TransactionType.valueOf(rs.getString("transaction_type")),
                userId,
                employeeId,
                rs.getBigDecimal("sub_total_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("final_total_amount"),
                paymentResult
        );
    }

    @Override
    public Optional<Bill> findById(int id) throws DatabaseOperationException {
        String sql = "SELECT * FROM bills WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBill(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding bill by ID: " + id + ". " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Bill> findBySerialNumberAndDate(String billSerialNumber, LocalDate billDate) throws DatabaseOperationException {
        String sql = "SELECT * FROM bills WHERE bill_serial_number = ? AND bill_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, billSerialNumber);
            pstmt.setDate(2, Date.valueOf(billDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBill(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding bill by serial number and date: " + billSerialNumber + " " + billDate + ". " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<DailySalesReportItemDTO> findSalesDataByDateAndType(LocalDate date, TransactionType transactionType) throws DatabaseOperationException {
        // This is a placeholder implementation. You'll need to write the actual SQL query
        // and map the results to DailySalesReportItemDTO.
        // The query should join bills and bill_items to aggregate sales data.
        String sql = "SELECT bi.item_code, bi.item_name_at_sale, SUM(bi.quantity_purchased) AS total_quantity, " +
                "SUM(bi.total_price_for_item) AS total_revenue " +
                "FROM bill_items bi JOIN bills b ON bi.bill_id = b.id " +
                "WHERE DATE(b.bill_date) = ? ";

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(date));

        if (transactionType != null) {
            sql += "AND b.transaction_type = ? ";
            params.add(transactionType.name());
        }
        sql += "GROUP BY bi.item_code, bi.item_name_at_sale ORDER BY total_revenue DESC";

        List<DailySalesReportItemDTO> salesData = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    salesData.add(new DailySalesReportItemDTO(
                            rs.getString("item_code"),
                            rs.getString("item_name_at_sale"),
                            rs.getInt("total_quantity"),
                            rs.getBigDecimal("total_revenue")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching daily sales data: " + e.getMessage(), e);
        }
        return salesData;
    }


    @Override
    public List<BillSummaryDTO> findAllBillSummaries(LocalDate startDate, LocalDate endDate, TransactionType transactionType) throws DatabaseOperationException {
        List<BillSummaryDTO> billSummaries = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT id, bill_serial_number, bill_date, transaction_type, user_id, final_total_amount FROM bills WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (startDate != null) { sqlBuilder.append("AND DATE(bill_date) >= ? "); params.add(Date.valueOf(startDate)); }
        if (endDate != null) { sqlBuilder.append("AND DATE(bill_date) <= ? "); params.add(Date.valueOf(endDate)); }
        if (transactionType != null) { sqlBuilder.append("AND transaction_type = ? "); params.add(transactionType.name()); }
        sqlBuilder.append("ORDER BY bill_date DESC");
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            for (Object param : params) { pstmt.setObject(paramIndex++, param); }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Retrieve user_id as Integer for BillSummaryDTO
                Integer userId = rs.getObject("user_id", Integer.class);
                billSummaries.add(new BillSummaryDTO(
                        rs.getString("bill_serial_number"),
                        rs.getTimestamp("bill_date").toLocalDateTime(),
                        TransactionType.valueOf(rs.getString("transaction_type")),
                        userId, // Now passing Integer to BillSummaryDTO
                        rs.getBigDecimal("final_total_amount")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching bill summaries", e);
        }
        return billSummaries;
    }

    @Override
    public Optional<Bill> findTopByBillDateOrderBySerialNumberDesc(LocalDate billDate) throws DatabaseOperationException {
        // Implement this method to fetch the bill with the highest serial number for a specific date
        // This is typically used by findNextBillSerialNumberForToday() to get the last serial.
        // It might be redundant if findNextBillSerialNumberForToday() already fetches it directly.
        // However, the interface requires it, so it needs an implementation.
        String sql = "SELECT * FROM bills WHERE bill_date = ? ORDER BY bill_serial_number DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(billDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBill(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding top bill by date and serial number: " + billDate + ". " + e.getMessage(), e);
        }
        return Optional.empty();
    }
}