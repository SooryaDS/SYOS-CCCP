package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ShelfStockRepository;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShelfStockRepositoryImpl implements ShelfStockRepository {

    @Override
    public ShelfStock saveOrUpdate(ShelfStock shelfStock) throws DatabaseOperationException {
        String sql = "INSERT INTO shelf_stock (item_code, batch_id, quantity_on_shelf, last_stocked_date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE quantity_on_shelf = quantity_on_shelf + VALUES(quantity_on_shelf), last_stocked_date = VALUES(last_stocked_date)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, shelfStock.getItemCode());
            pstmt.setInt(2, shelfStock.getBatchId());
            pstmt.setInt(3, shelfStock.getQuantityOnShelf());
            pstmt.setTimestamp(4, Timestamp.valueOf(shelfStock.getLastStockedDate()));
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    shelfStock.setShelfStockId(generatedKeys.getInt(1));
                } else {
                    if (shelfStock.getShelfStockId() == 0) {
                        findByItemCodeAndBatchId(shelfStock.getItemCode(), shelfStock.getBatchId())
                                .ifPresent(ss -> shelfStock.setShelfStockId(ss.getShelfStockId()));
                    }
                }
            }
            return shelfStock;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving or updating shelf stock", e);
        }
    }

    @Override
    public Optional<ShelfStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException {
        String sql = "SELECT * FROM shelf_stock WHERE item_code = ? AND batch_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            pstmt.setInt(2, batchId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new ShelfStock(
                        rs.getInt("shelf_stock_id"),
                        rs.getString("item_code"),
                        rs.getInt("batch_id"),
                        rs.getInt("quantity_on_shelf"),
                        rs.getTimestamp("last_stocked_date").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding shelf stock by item code and batch ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ShelfStock> findByItemCode(String itemCode) throws DatabaseOperationException {
        List<ShelfStock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM shelf_stock WHERE item_code = ? AND quantity_on_shelf > 0 ORDER BY last_stocked_date ASC, batch_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                stocks.add(new ShelfStock(
                        rs.getInt("shelf_stock_id"),
                        rs.getString("item_code"),
                        rs.getInt("batch_id"),
                        rs.getInt("quantity_on_shelf"),
                        rs.getTimestamp("last_stocked_date").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding shelf stocks by item code: " + itemCode, e);
        }
        return stocks;
    }

    @Override
    public ShelfStock update(ShelfStock shelfStock) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return update(shelfStock, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting connection for shelf stock update", e);
        }
    }

    @Override
    public void delete(ShelfStock shelfStock) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            delete(shelfStock, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting connection for shelf stock delete", e);
        }
    }

    @Override
    public ShelfStock update(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException {
        String sql = "UPDATE shelf_stock SET quantity_on_shelf = ?, last_stocked_date = ? WHERE shelf_stock_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shelfStock.getQuantityOnShelf());
            pstmt.setTimestamp(2, Timestamp.valueOf(shelfStock.getLastStockedDate()));
            pstmt.setInt(3, shelfStock.getShelfStockId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Updating shelf stock failed, no record found with ID: " + shelfStock.getShelfStockId());
            }
            return shelfStock;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating shelf stock with ID: " + shelfStock.getShelfStockId(), e);
        }
    }

    @Override
    public void delete(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException {
        String sql = "DELETE FROM shelf_stock WHERE shelf_stock_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shelfStock.getShelfStockId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting shelf stock with ID: " + shelfStock.getShelfStockId(), e);
        }
    }
}
