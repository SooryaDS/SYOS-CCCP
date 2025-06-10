package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.StockBatchRepository;
import com.syos.domain.model.StockBatch;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StockBatchRepositoryImpl implements StockBatchRepository {
    // ... all existing methods (save, findById, findByItemCode, findAll, update) remain the same ...
    @Override
    public StockBatch save(StockBatch batch) throws DatabaseOperationException {
        String sql = "INSERT INTO stock_batches (item_code, purchase_date, quantity_received, current_quantity_in_store, expiry_date) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, batch.getItemCode());
            pstmt.setDate(2, Date.valueOf(batch.getPurchaseDate()));
            pstmt.setInt(3, batch.getQuantityReceived());
            pstmt.setInt(4, batch.getCurrentQuantityInStore());
            if (batch.getExpiryDate() != null) {
                pstmt.setDate(5, Date.valueOf(batch.getExpiryDate()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new DatabaseOperationException("Creating stock batch failed, no rows affected.");
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) batch.setBatchId(generatedKeys.getInt(1));
                else throw new DatabaseOperationException("Creating stock batch failed, no ID obtained.");
            }
            return batch;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving stock batch for item: " + batch.getItemCode(), e);
        }
    }
    @Override
    public Optional<StockBatch> findById(int batchId) throws DatabaseOperationException {
        String sql = "SELECT batch_id, item_code, purchase_date, quantity_received, current_quantity_in_store, expiry_date " +
                "FROM stock_batches WHERE batch_id = ?";
        StockBatch batch = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, batchId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                LocalDate expiry = rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null;
                batch = new StockBatch(
                        rs.getInt("batch_id"), rs.getString("item_code"),
                        rs.getDate("purchase_date").toLocalDate(), rs.getInt("quantity_received"),
                        rs.getInt("current_quantity_in_store"), expiry);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding stock batch by ID: " + batchId, e);
        }
        return Optional.ofNullable(batch);
    }
    @Override
    public List<StockBatch> findByItemCode(String itemCode) throws DatabaseOperationException {
        List<StockBatch> batches = new ArrayList<>();
        String sql = "SELECT batch_id, item_code, purchase_date, quantity_received, current_quantity_in_store, expiry_date " +
                "FROM stock_batches WHERE item_code = ? AND current_quantity_in_store > 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemCode);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LocalDate expiry = rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null;
                batches.add(new StockBatch(
                        rs.getInt("batch_id"), rs.getString("item_code"),
                        rs.getDate("purchase_date").toLocalDate(), rs.getInt("quantity_received"),
                        rs.getInt("current_quantity_in_store"), expiry));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding stock batches by item code: " + itemCode, e);
        }
        return batches;
    }
    @Override
    public List<StockBatch> findAll() throws DatabaseOperationException {
        List<StockBatch> batches = new ArrayList<>();
        String sql = "SELECT batch_id, item_code, purchase_date, quantity_received, current_quantity_in_store, expiry_date " +
                "FROM stock_batches";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                LocalDate expiry = rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null;
                batches.add(new StockBatch(
                        rs.getInt("batch_id"), rs.getString("item_code"),
                        rs.getDate("purchase_date").toLocalDate(), rs.getInt("quantity_received"),
                        rs.getInt("current_quantity_in_store"), expiry));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding all stock batches", e);
        }
        return batches;
    }
    @Override
    public StockBatch update(StockBatch batch) throws DatabaseOperationException {
        String sql = "UPDATE stock_batches SET item_code = ?, purchase_date = ?, quantity_received = ?, " +
                "current_quantity_in_store = ?, expiry_date = ? WHERE batch_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, batch.getItemCode());
            pstmt.setDate(2, Date.valueOf(batch.getPurchaseDate()));
            pstmt.setInt(3, batch.getQuantityReceived());
            pstmt.setInt(4, batch.getCurrentQuantityInStore());
            if (batch.getExpiryDate() != null) pstmt.setDate(5, Date.valueOf(batch.getExpiryDate()));
            else pstmt.setNull(5, Types.DATE);
            pstmt.setInt(6, batch.getBatchId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new DatabaseOperationException("Updating stock batch failed, no batch found with ID: " + batch.getBatchId());
            return batch;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating stock batch with ID: " + batch.getBatchId(), e);
        }
    }


    @Override
    public List<StockBatch> findExpiredBatches(LocalDate currentDate) throws DatabaseOperationException {
        List<StockBatch> expiredBatches = new ArrayList<>();
        // Finds batches where expiry_date is not null and is before the current date.
        String sql = "SELECT * FROM stock_batches WHERE expiry_date IS NOT NULL AND expiry_date < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(currentDate));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                expiredBatches.add(new StockBatch(
                        rs.getInt("batch_id"),
                        rs.getString("item_code"),
                        rs.getDate("purchase_date").toLocalDate(),
                        rs.getInt("quantity_received"),
                        rs.getInt("current_quantity_in_store"),
                        rs.getDate("expiry_date").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding expired stock batches.", e);
        }
        return expiredBatches;
    }

    @Override
    public int deleteBatchesById(List<Integer> batchIds) throws DatabaseOperationException {
        if (batchIds == null || batchIds.isEmpty()) {
            return 0;
        }
        // This creates a query like "DELETE FROM stock_batches WHERE batch_id IN (?,?,?)"
        String placeholders = batchIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "DELETE FROM stock_batches WHERE batch_id IN (" + placeholders + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < batchIds.size(); i++) {
                pstmt.setInt(i + 1, batchIds.get(i));
            }
            // IMPORTANT: Deleting from stock_batches can fail if shelf_stock or website_stock
            // has items from this batch. This assumes expired stock is first removed from shelves/website,
            // or that foreign keys are set to ON DELETE SET NULL or ON DELETE CASCADE.
            // A simple implementation might get a foreign key constraint violation here, which is
            // a business process issue (expired stock should be removed from sale points first).
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting expired batches.", e);
        }
    }
}