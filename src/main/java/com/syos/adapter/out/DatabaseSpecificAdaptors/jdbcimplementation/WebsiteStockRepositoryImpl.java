package com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.WebsiteStockRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.WebsiteStock;
import com.syos.domain.model.Item;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebsiteStockRepositoryImpl implements WebsiteStockRepository {

    private final ItemRepository itemRepository;

    public WebsiteStockRepositoryImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    private WebsiteStock mapRowToWebsiteStock(ResultSet rs) throws SQLException, DatabaseOperationException {
        Long websiteStockId = rs.getLong("website_stock_id");
        String itemCode = rs.getString("item_code");
        int quantityAvailableOnline = rs.getInt("quantity_available_online");
        Timestamp lastUpdatedTimestamp = rs.getTimestamp("last_updated_date");
        LocalDateTime lastUpdatedDate = (lastUpdatedTimestamp != null) ? lastUpdatedTimestamp.toLocalDateTime() : null;

        Item item = itemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new DatabaseOperationException("Referenced Item with code " + itemCode + " not found for WebsiteStock ID " + websiteStockId));

        return new WebsiteStock(websiteStockId, item, quantityAvailableOnline, lastUpdatedDate);
    }

    private WebsiteStock executeSave(WebsiteStock websiteStock, Connection existingConn) throws DatabaseOperationException {
        Connection conn = null;
        boolean closeConn = false;

        try {
            conn = (existingConn != null) ? existingConn : DatabaseConnection.getConnection();
            closeConn = (existingConn == null);

            if (websiteStock.getWebsiteStockId() == null || websiteStock.getWebsiteStockId() == 0) {
                String sql = "INSERT INTO website_stock (item_code, quantity_available_online, last_updated_date) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, websiteStock.getItem().getItemCode());
                    stmt.setInt(2, websiteStock.getQuantityAvailableOnline());
                    stmt.setTimestamp(3, Timestamp.valueOf(websiteStock.getLastUpdatedDate()));

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new DatabaseOperationException("Creating website stock failed, no rows affected.");
                    }

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            websiteStock.setWebsiteStockId(generatedKeys.getLong(1));
                        } else {
                            throw new DatabaseOperationException("Creating website stock failed, no ID obtained.");
                        }
                    }
                }
            } else {
                String sql = "UPDATE website_stock SET quantity_available_online = ?, last_updated_date = ? WHERE website_stock_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, websiteStock.getQuantityAvailableOnline());
                    stmt.setTimestamp(2, Timestamp.valueOf(websiteStock.getLastUpdatedDate()));
                    stmt.setLong(3, websiteStock.getWebsiteStockId());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        System.err.println("Warning: Updating website stock failed, no rows affected for ID: " + websiteStock.getWebsiteStockId() + ".");
                    }
                }
            }
            return websiteStock;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving/updating website stock: " + e.getMessage(), e);
        } finally {
            if (closeConn) {
                try { if (conn != null) conn.close(); } catch (SQLException e) { System.err.println("Error closing connection: " + e.getMessage()); }
            }
        }
    }

    @Override
    public WebsiteStock save(WebsiteStock websiteStock) throws DatabaseOperationException {
        return executeSave(websiteStock, null);
    }

    @Override
    public WebsiteStock save(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException {
        return executeSave(websiteStock, conn);
    }

    @Override
    public Optional<WebsiteStock> findById(Long id) throws DatabaseOperationException {
        String sql = "SELECT * FROM website_stock WHERE website_stock_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToWebsiteStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding website stock by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<WebsiteStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException {
        String sql = "SELECT * FROM website_stock WHERE item_code = ? AND batch_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemCode);
            stmt.setInt(2, batchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToWebsiteStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding website stock by item code and batch ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<WebsiteStock> findByItemCode(String itemCode) throws DatabaseOperationException {
        String sql = "SELECT * FROM website_stock WHERE item_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToWebsiteStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding website stock by item code: " + itemCode + ". " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<WebsiteStock> findByItem(Item item) throws DatabaseOperationException {
        String sql = "SELECT * FROM website_stock WHERE item_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getItemCode());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToWebsiteStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding website stock by item: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<WebsiteStock> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM website_stock";
        List<WebsiteStock> websiteStocks = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) websiteStocks.add(mapRowToWebsiteStock(rs));
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all website stock: " + e.getMessage(), e);
        }
        return websiteStocks;
    }
    @Override
    public Optional<WebsiteStock> findByItemForUpdate(Item item, Connection conn) throws DatabaseOperationException {
        String sql = "SELECT * FROM website_stock WHERE item_code = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getItemCode());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToWebsiteStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding website stock for update: " + e.getMessage(), e);
        }
        return Optional.empty();
    }


    @Override
    public int getAvailableQuantity(String itemCode) throws DatabaseOperationException {
        String sql = "SELECT COALESCE(SUM(quantity_available_online), 0) FROM website_stock WHERE item_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting available website quantity for item code " + itemCode + ": " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public WebsiteStock update(WebsiteStock websiteStock) throws DatabaseOperationException {
        return save(websiteStock);
    }

    @Override
    public WebsiteStock update(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException {
        return save(websiteStock, conn);
    }

    private void executeDelete(Long id, Connection existingConn) throws DatabaseOperationException {
        Connection conn = null;
        boolean closeConn = false;

        try {
            conn = (existingConn != null) ? existingConn : DatabaseConnection.getConnection();
            closeConn = (existingConn == null);

            String sql = "DELETE FROM website_stock WHERE website_stock_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting website stock by ID: " + e.getMessage(), e);
        } finally {
            if (closeConn) {
                try { if (conn != null) conn.close(); } catch (SQLException e) { System.err.println("Error closing connection: " + e.getMessage()); }
            }
        }
    }

    @Override
    public void delete(Long id) throws DatabaseOperationException {
        executeDelete(id, null);
    }

    @Override
    public void delete(WebsiteStock websiteStock) throws DatabaseOperationException {
        if (websiteStock == null || websiteStock.getWebsiteStockId() == null)
            throw new IllegalArgumentException("WebsiteStock or its ID cannot be null for deletion.");
        executeDelete(websiteStock.getWebsiteStockId(), null);
    }

    @Override
    public void delete(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException {
        if (websiteStock == null || websiteStock.getWebsiteStockId() == null)
            throw new IllegalArgumentException("WebsiteStock or its ID cannot be null for deletion.");
        executeDelete(websiteStock.getWebsiteStockId(), conn);
    }

    @Override
    public void deleteById(Long id) throws DatabaseOperationException {
        executeDelete(id, null);
    }
}
