package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineOrderRepository;
import com.syos.domain.model.Item;
import com.syos.domain.model.OnlineOrder;
import com.syos.domain.model.OnlineOrderItem;
import com.syos.domain.enums.OrderStatus;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OnlineOrderRepositoryImpl implements OnlineOrderRepository {

    private final ItemRepository itemRepository; // To reconstruct Item objects when loading OnlineOrderItems

    public OnlineOrderRepositoryImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public OnlineOrder save(OnlineOrder order) throws DatabaseOperationException {
        // Updated SQL to include discount_amount
        String orderSql = "INSERT INTO online_orders (order_id, online_user_id, order_status, creation_date, last_modified_date, shipping_address, calculated_total_amount, discount_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"; // Added one more '?' for discount_amount
        String orderItemSql = "INSERT INTO online_order_items (order_id, item_code, quantity, price_at_addition, line_total) " +
                "VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Transaction

            // Save Order Header
            try (PreparedStatement pstmtOrder = conn.prepareStatement(orderSql)) {
                pstmtOrder.setString(1, order.getOrderId());
                pstmtOrder.setInt(2, order.getOnlineUserId());
                pstmtOrder.setString(3, order.getStatus().name());
                pstmtOrder.setTimestamp(4, Timestamp.valueOf(order.getCreationDate()));
                pstmtOrder.setTimestamp(5, Timestamp.valueOf(order.getLastModifiedDate()));
                pstmtOrder.setString(6, order.getShippingAddress());
                pstmtOrder.setBigDecimal(7, order.getCalculatedTotalAmount());
                pstmtOrder.setBigDecimal(8, order.getDiscountAmount()); // NEW: Set discount_amount
                pstmtOrder.executeUpdate();
            }

            // Save Order Items
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                try (PreparedStatement pstmtItems = conn.prepareStatement(orderItemSql)) {
                    for (OnlineOrderItem oItem : order.getItems()) {
                        pstmtItems.setString(1, order.getOrderId());
                        pstmtItems.setString(2, oItem.getItemCode());
                        pstmtItems.setInt(3, oItem.getQuantity());
                        pstmtItems.setBigDecimal(4, oItem.getPriceAtAddition());
                        pstmtItems.setBigDecimal(5, oItem.getLineTotal());
                        pstmtItems.addBatch();
                    }
                    pstmtItems.executeBatch();
                }
            }
            conn.commit();
            return order;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* log */ }
            throw new DatabaseOperationException("Error saving online order: " + order.getOrderId() + ". " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { /* log */ }
        }
    }

    private OnlineOrder mapRowToOnlineOrder(ResultSet rs) throws SQLException {
        // Updated to fetch discount_amount and pass it to the constructor
        return new OnlineOrder(
                rs.getString("order_id"),
                rs.getInt("online_user_id"),
                OrderStatus.valueOf(rs.getString("order_status")),
                rs.getTimestamp("creation_date").toLocalDateTime(),
                rs.getTimestamp("last_modified_date").toLocalDateTime(),
                rs.getString("shipping_address"),
                rs.getBigDecimal("calculated_total_amount"),
                rs.getBigDecimal("discount_amount") // NEW: Fetch and pass discount_amount
        );
    }

    private List<OnlineOrderItem> findItemsForOrder(String orderId, Connection conn) throws SQLException, DatabaseOperationException {
        List<OnlineOrderItem> items = new ArrayList<>();
        String sql = "SELECT order_item_id, item_code, quantity, price_at_addition, line_total FROM online_order_items WHERE order_id = ?"; // Added line_total to select
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String itemCode = rs.getString("item_code");
                Item domainItem = itemRepository.findByItemCode(itemCode)
                        .orElseThrow(() -> new DatabaseOperationException("Corrupt data: Item " + itemCode + " not found for order item."));
                items.add(new OnlineOrderItem(
                        rs.getInt("order_item_id"),
                        orderId,
                        domainItem,
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price_at_addition")
                        // No need to pass line_total here, it's recalculated in OnlineOrderItem constructor
                ));
            }
        }
        return items;
    }


    @Override
    public Optional<OnlineOrder> findById(String orderId) throws DatabaseOperationException {
        // Updated SQL to include discount_amount
        String sql = "SELECT order_id, online_user_id, order_status, creation_date, last_modified_date, shipping_address, calculated_total_amount, discount_amount FROM online_orders WHERE order_id = ?";
        OnlineOrder order = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, orderId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    order = mapRowToOnlineOrder(rs);
                    order.setItems(findItemsForOrder(orderId, conn)); // Load items within the same connection
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding online order by ID: " + orderId + ". " + e.getMessage(), e);
        }
        return Optional.ofNullable(order);
    }


    @Override
    public List<OnlineOrder> findByUserIdAndStatus(int onlineUserId, OrderStatus status) throws DatabaseOperationException {
        List<OnlineOrder> orders = new ArrayList<>();
        // Updated SQL to include discount_amount
        String sql = "SELECT order_id, online_user_id, order_status, creation_date, last_modified_date, shipping_address, calculated_total_amount, discount_amount FROM online_orders WHERE online_user_id = ? AND order_status = ?";
        try (Connection conn = DatabaseConnection.getConnection()){
            try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, onlineUserId);
                pstmt.setString(2, status.name());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    OnlineOrder order = mapRowToOnlineOrder(rs);
                    order.setItems(findItemsForOrder(order.getOrderId(), conn)); // Load items
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding online orders for user: " + onlineUserId + " with status: " + status + ". " + e.getMessage(), e);
        }
        return orders;
    }


    @Override
    public OnlineOrder update(OnlineOrder order) throws DatabaseOperationException {
        // Updated SQL to include discount_amount
        String orderSql = "UPDATE online_orders SET order_status = ?, last_modified_date = ?, shipping_address = ?, calculated_total_amount = ?, discount_amount = ? " +
                "WHERE order_id = ?"; // Added discount_amount update
        String deleteItemsSql = "DELETE FROM online_order_items WHERE order_id = ?";
        String insertItemSql = "INSERT INTO online_order_items (order_id, item_code, quantity, price_at_addition, line_total) " +
                "VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Update Order Header
            try (PreparedStatement pstmtOrder = conn.prepareStatement(orderSql)) {
                pstmtOrder.setString(1, order.getStatus().name());
                pstmtOrder.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now())); // Always update last_modified
                pstmtOrder.setString(3, order.getShippingAddress());
                pstmtOrder.setBigDecimal(4, order.getCalculatedTotalAmount());
                pstmtOrder.setBigDecimal(5, order.getDiscountAmount()); // NEW: Set discount_amount for update
                pstmtOrder.setString(6, order.getOrderId());
                int affected = pstmtOrder.executeUpdate();
                if (affected == 0) throw new DatabaseOperationException("Order not found for update: " + order.getOrderId());
            }

            // Delete existing items
            try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteItemsSql)) {
                pstmtDelete.setString(1, order.getOrderId());
                pstmtDelete.executeUpdate();
            }

            // Insert new items
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                try (PreparedStatement pstmtItems = conn.prepareStatement(insertItemSql)) {
                    for (OnlineOrderItem oItem : order.getItems()) {
                        pstmtItems.setString(1, order.getOrderId());
                        pstmtItems.setString(2, oItem.getItemCode());
                        pstmtItems.setInt(3, oItem.getQuantity());
                        pstmtItems.setBigDecimal(4, oItem.getPriceAtAddition());
                        pstmtItems.setBigDecimal(5, oItem.getLineTotal());
                        pstmtItems.addBatch();
                    }
                    pstmtItems.executeBatch();
                }
            }
            conn.commit();
            return order;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* log */ }
            throw new DatabaseOperationException("Error updating online order: " + order.getOrderId() + ". " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { /* log */ }
        }
    }
}
