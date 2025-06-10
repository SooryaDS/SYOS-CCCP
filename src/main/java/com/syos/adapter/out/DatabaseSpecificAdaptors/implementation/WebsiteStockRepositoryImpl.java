package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.WebsiteStockRepository;
import com.syos.domain.model.WebsiteStock;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebsiteStockRepositoryImpl implements WebsiteStockRepository {

    @Override
    public WebsiteStock saveOrUpdate(WebsiteStock websiteStock) throws DatabaseOperationException {
        String sql = "INSERT INTO website_stock (item_code, batch_id, quantity_available_online, last_updated_date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE quantity_available_online = quantity_available_online + VALUES(quantity_available_online), last_updated_date = VALUES(last_updated_date)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, websiteStock.getItemCode());
            pstmt.setInt(2, websiteStock.getBatchId());
            pstmt.setInt(3, websiteStock.getQuantityAvailableOnline());
            pstmt.setTimestamp(4, Timestamp.valueOf(websiteStock.getLastUpdatedDate()));
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    websiteStock.setWebsiteStockId(generatedKeys.getInt(1));
                } else {
                    if (websiteStock.getWebsiteStockId() == 0) {
                        findByItemCodeAndBatchId(websiteStock.getItemCode(), websiteStock.getBatchId())
                                .ifPresent(ss -> websiteStock.setWebsiteStockId(ss.getWebsiteStockId()));
                    }
                }
            }
            return websiteStock;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving or updating website stock", e);
        }
    }

    @Override
    public Optional<WebsiteStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException {
        String sql = "SELECT * FROM website_stock WHERE item_code = ? AND batch_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            pstmt.setInt(2, batchId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new WebsiteStock(
                        rs.getInt("website_stock_id"),
                        rs.getString("item_code"),
                        rs.getInt("batch_id"),
                        rs.getInt("quantity_available_online"),
                        rs.getTimestamp("last_updated_date").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding website stock by item code and batch ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<WebsiteStock> findByItemCode(String itemCode) throws DatabaseOperationException {
        List<WebsiteStock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM website_stock WHERE item_code = ? AND quantity_available_online > 0 ORDER BY last_updated_date ASC, batch_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                stocks.add(new WebsiteStock(
                        rs.getInt("website_stock_id"),
                        rs.getString("item_code"),
                        rs.getInt("batch_id"),
                        rs.getInt("quantity_available_online"),
                        rs.getTimestamp("last_updated_date").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding website stocks by item code: " + itemCode, e);
        }
        return stocks;
    }

    @Override
    public WebsiteStock update(WebsiteStock websiteStock) throws DatabaseOperationException {
        try(Connection conn = DatabaseConnection.getConnection()) {
            return update(websiteStock, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting connection for website stock update", e);
        }
    }

    @Override
    public void delete(WebsiteStock websiteStock) throws DatabaseOperationException {
        try(Connection conn = DatabaseConnection.getConnection()) {
            delete(websiteStock, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting connection for website stock delete", e);
        }
    }

    @Override
    public WebsiteStock update(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException {
        String sql = "UPDATE website_stock SET quantity_available_online = ?, last_updated_date = ? WHERE website_stock_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, websiteStock.getQuantityAvailableOnline());
            pstmt.setTimestamp(2, Timestamp.valueOf(websiteStock.getLastUpdatedDate()));
            pstmt.setInt(3, websiteStock.getWebsiteStockId());
            if (pstmt.executeUpdate() == 0) {
                throw new DatabaseOperationException("Updating website stock failed, no record found with ID: " + websiteStock.getWebsiteStockId());
            }
            return websiteStock;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating website stock with ID: " + websiteStock.getWebsiteStockId(), e);
        }
    }

    @Override
    public void delete(WebsiteStock websiteStock, Connection conn) throws DatabaseOperationException {
        String sql = "DELETE FROM website_stock WHERE website_stock_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, websiteStock.getWebsiteStockId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting website stock with ID: " + websiteStock.getWebsiteStockId(), e);
        }
    }

    @Override
    public int getTotalStockByItemCode(String itemCode) throws DatabaseOperationException {
        String sql = "SELECT SUM(quantity_available_online) FROM website_stock WHERE item_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting total website stock for item: " + itemCode, e);
        }
        return 0;
    }
}