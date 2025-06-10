package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.BillRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.domain.model.Bill;
import com.syos.domain.model.BillItem;
import com.syos.domain.enums.TransactionType;
import com.syos.adapter.dto.BillSummaryDTO;
import com.syos.adapter.dto.DailySalesReportItemDTO;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BillRepositoryImpl implements BillRepository {

    private final ItemRepository itemRepository;

    public BillRepositoryImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public int findNextBillSerialNumberForToday() throws DatabaseOperationException {
        String sql = "SELECT IFNULL(MAX(bill_serial_number), 0) + 1 FROM bills WHERE DATE(bill_date) = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching next bill serial number.", e);
        }
        return 1;
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
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* Log error */ }
            if(e instanceof DatabaseOperationException) throw (DatabaseOperationException) e;
            throw new DatabaseOperationException("Error saving bill.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { /* Log error */ }
        }
    }

    @Override
    public Bill save(Bill bill, Connection conn) throws DatabaseOperationException {
        String billSql = "INSERT INTO bills (bill_serial_number, bill_date, transaction_type, user_id, employee_id, sub_total_amount, discount_amount, final_total_amount, cash_tendered, change_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String billItemSql = "INSERT INTO bill_items (bill_serial_number, bill_date, item_code, item_name_at_sale, quantity_purchased, price_per_unit_at_sale, total_price_for_item) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement billPstmt = conn.prepareStatement(billSql);
             PreparedStatement billItemPstmt = conn.prepareStatement(billItemSql)) {
            billPstmt.setInt(1, bill.getBillSerialNumber());
            billPstmt.setTimestamp(2, Timestamp.valueOf(bill.getBillDate()));
            billPstmt.setString(3, bill.getTransactionType().name());
            billPstmt.setString(4, bill.getUserId());
            billPstmt.setString(5, bill.getEmployeeId());
            billPstmt.setBigDecimal(6, bill.getSubTotalAmount());
            billPstmt.setBigDecimal(7, bill.getDiscountAmount());
            billPstmt.setBigDecimal(8, bill.getFinalTotalAmount());
            billPstmt.setBigDecimal(9, bill.getCashTendered());
            billPstmt.setBigDecimal(10, bill.getChangeAmount());
            if (billPstmt.executeUpdate() == 0) throw new SQLException("Creating bill failed, no rows affected.");

            for (BillItem bi : bill.getBillItems()) {
                billItemPstmt.setInt(1, bill.getBillSerialNumber());
                billItemPstmt.setTimestamp(2, Timestamp.valueOf(bill.getBillDate()));
                billItemPstmt.setString(3, bi.getItem().getItemCode());
                billItemPstmt.setString(4, bi.getItemNameAtSale());
                billItemPstmt.setInt(5, bi.getQuantityPurchased());
                billItemPstmt.setBigDecimal(6, bi.getPricePerUnitAtSale());
                billItemPstmt.setBigDecimal(7, bi.getTotalPriceForItem());
                billItemPstmt.addBatch();
            }
            billItemPstmt.executeBatch();
            return bill;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving bill within transaction", e);
        }
    }

    @Override
    public Optional<Bill> findBySerialNumberAndDate(int billSerialNumber, LocalDate billDate) throws DatabaseOperationException {
        String billSql = "SELECT * FROM bills WHERE bill_serial_number = ? AND DATE(bill_date) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement billPstmt = conn.prepareStatement(billSql)) {
            billPstmt.setInt(1, billSerialNumber);
            billPstmt.setDate(2, Date.valueOf(billDate));
            ResultSet rs = billPstmt.executeQuery();
            if (rs.next()) {
                Bill bill = new Bill(rs.getInt("bill_serial_number"), rs.getTimestamp("bill_date").toLocalDateTime(), TransactionType.valueOf(rs.getString("transaction_type")), rs.getString("user_id"), rs.getString("employee_id"), rs.getBigDecimal("sub_total_amount"), rs.getBigDecimal("discount_amount"), rs.getBigDecimal("final_total_amount"), rs.getBigDecimal("cash_tendered"), rs.getBigDecimal("change_amount"));
                return Optional.of(bill);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding bill by serial number and date", e);
        }
        return Optional.empty();
    }

    @Override
    public List<DailySalesReportItemDTO> findSalesDataByDateAndType(LocalDate date, TransactionType transactionType) throws DatabaseOperationException {
        List<DailySalesReportItemDTO> salesReportItems = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT bi.item_code, i.name AS item_name, SUM(bi.quantity_purchased) AS total_quantity_sold, SUM(bi.total_price_for_item) AS total_revenue_for_item FROM bill_items bi JOIN bills b ON bi.bill_serial_number = b.bill_serial_number AND DATE(bi.bill_date) = DATE(b.bill_date) JOIN items i ON bi.item_code = i.item_code WHERE DATE(b.bill_date) = ? ");
        if (transactionType != null) {
            sqlBuilder.append("AND b.transaction_type = ? ");
        }
        sqlBuilder.append("GROUP BY bi.item_code, i.name ORDER BY item_name");
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            pstmt.setDate(1, Date.valueOf(date));
            if (transactionType != null) {
                pstmt.setString(2, transactionType.name());
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                salesReportItems.add(new DailySalesReportItemDTO(rs.getString("item_code"), rs.getString("item_name"), rs.getInt("total_quantity_sold"), rs.getBigDecimal("total_revenue_for_item")));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching daily sales data for date: " + date, e);
        }
        return salesReportItems;
    }

    @Override
    public List<BillSummaryDTO> findAllBillSummaries(LocalDate startDate, LocalDate endDate, TransactionType transactionType) throws DatabaseOperationException {
        List<BillSummaryDTO> billSummaries = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT bill_serial_number, bill_date, transaction_type, user_id, final_total_amount FROM bills WHERE 1=1 ");
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
                billSummaries.add(new BillSummaryDTO(rs.getInt("bill_serial_number"), rs.getTimestamp("bill_date").toLocalDateTime(), TransactionType.valueOf(rs.getString("transaction_type")), rs.getString("user_id"), rs.getBigDecimal("final_total_amount")));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching bill summaries", e);
        }
        return billSummaries;
    }

    @Override
    public Optional<Bill> findTopByBillDateOrderBySerialNumberDesc(LocalDate billDate) throws DatabaseOperationException {
        String sql = "SELECT * FROM bills WHERE DATE(bill_date) = ? ORDER BY bill_serial_number DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(billDate));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Bill bill = new Bill(rs.getInt("bill_serial_number"), rs.getTimestamp("bill_date").toLocalDateTime(), TransactionType.valueOf(rs.getString("transaction_type")), rs.getString("user_id"), rs.getString("employee_id"), rs.getBigDecimal("sub_total_amount"), rs.getBigDecimal("discount_amount"), rs.getBigDecimal("final_total_amount"), rs.getBigDecimal("cash_tendered"), rs.getBigDecimal("change_amount"));
                return Optional.of(bill);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding top bill by date and serial number.", e);
        }
        return Optional.empty();
    }
}