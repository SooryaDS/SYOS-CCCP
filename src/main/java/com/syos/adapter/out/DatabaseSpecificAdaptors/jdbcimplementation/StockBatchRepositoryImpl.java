package com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.StockBatchRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.StockBatch;
import com.syos.domain.model.Item;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StockBatchRepositoryImpl implements StockBatchRepository {

    private final ItemRepository itemRepository;

    public StockBatchRepositoryImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    private StockBatch mapRowToStockBatch(ResultSet rs) throws SQLException, DatabaseOperationException {
        Long batchId = rs.getLong("batch_id");
        String itemCode = rs.getString("item_code");
        int receivedQuantity = rs.getInt("received_quantity");
        int currentQuantityInStore = rs.getInt("current_quantity_in_store");
        Date expiryDateSql = rs.getDate("expiry_date");
        LocalDate expiryDate = (expiryDateSql != null) ? expiryDateSql.toLocalDate() : null;
        BigDecimal costPerUnit = rs.getBigDecimal("cost_per_unit");
        Date receivedDateSql = rs.getDate("received_date");
        LocalDate receivedDate = (receivedDateSql != null) ? receivedDateSql.toLocalDate() : null;

        Item item = itemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new DatabaseOperationException(
                        "Referenced Item with code " + itemCode + " not found for StockBatch ID " + batchId));

        return new StockBatch(batchId, item, receivedQuantity, currentQuantityInStore, expiryDate, costPerUnit, receivedDate);
    }

    // ================= Non-transactional methods =================
    @Override
    public StockBatch save(StockBatch batch) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(batch, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error obtaining connection: " + e.getMessage(), e);
        }
    }

    @Override
    public StockBatch update(StockBatch batch) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return update(batch, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error obtaining connection: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(StockBatch stockBatch) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            delete(stockBatch, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error obtaining connection: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<StockBatch> findById(Long id) throws DatabaseOperationException {
        String sql = "SELECT * FROM stock_batches WHERE batch_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToStockBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding stock batch by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }
    @Override
    public Optional<StockBatch> findByIdForUpdate(Long batchId, Connection conn) throws DatabaseOperationException {
        String sql = "SELECT * FROM stock_batches WHERE batch_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, batchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToStockBatch(rs));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching stock batch by ID for update: " + e.getMessage(), e);
        }
    }


    @Override
    public List<StockBatch> findByItem(Item item) throws DatabaseOperationException {
        String sql = "SELECT * FROM stock_batches WHERE item_code = ?";
        List<StockBatch> batches = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getItemCode());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapRowToStockBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding stock batches by item: " + e.getMessage(), e);
        }
        return batches;
    }

    // Non-transactional findAll now delegates to transactional version
    @Override
    public List<StockBatch> findAll() throws DatabaseOperationException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error obtaining connection: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StockBatch> findExpiredBatches(LocalDate currentDate) throws DatabaseOperationException {
        String sql = "SELECT * FROM stock_batches WHERE expiry_date <= ?";
        List<StockBatch> expiredBatches = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(currentDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expiredBatches.add(mapRowToStockBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding expired batches: " + e.getMessage(), e);
        }
        return expiredBatches;
    }

    @Override
    public int deleteBatchesById(List<Integer> batchIds) throws DatabaseOperationException {
        if (batchIds == null || batchIds.isEmpty()) return 0;
        StringBuilder sql = new StringBuilder("DELETE FROM stock_batches WHERE batch_id IN (");
        for (int i = 0; i < batchIds.size(); i++) {
            sql.append("?");
            if (i < batchIds.size() - 1) sql.append(",");
        }
        sql.append(")");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < batchIds.size(); i++) {
                stmt.setLong(i + 1, batchIds.get(i).longValue());
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting batches by IDs: " + e.getMessage(), e);
        }
    }


    // ================= Transactional methods =================

    public StockBatch save(StockBatch batch, Connection conn) throws DatabaseOperationException {
        String sql = "INSERT INTO stock_batches (item_code, received_quantity, current_quantity_in_store, expiry_date, cost_per_unit, received_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, batch.getItem().getItemCode());
            stmt.setInt(2, batch.getReceivedQuantity());
            stmt.setInt(3, batch.getCurrentQuantityInStore());
            stmt.setDate(4, Date.valueOf(batch.getExpiryDate()));
            stmt.setBigDecimal(5, batch.getCostPerUnit());
            stmt.setDate(6, Date.valueOf(batch.getReceivedDate()));
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0)
                throw new DatabaseOperationException("Creating stock batch failed, no rows affected.");
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) batch.setBatchId(generatedKeys.getLong(1));
                else throw new DatabaseOperationException("Creating stock batch failed, no ID obtained.");
            }
            return batch;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving stock batch: " + e.getMessage(), e);
        }
    }

    public StockBatch update(StockBatch batch, Connection conn) throws DatabaseOperationException {
        String sql = "UPDATE stock_batches SET current_quantity_in_store = ?, expiry_date = ?, cost_per_unit = ?, received_date = ? WHERE batch_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, batch.getCurrentQuantityInStore());
            stmt.setDate(2, Date.valueOf(batch.getExpiryDate()));
            stmt.setBigDecimal(3, batch.getCostPerUnit());
            stmt.setDate(4, Date.valueOf(batch.getReceivedDate()));
            stmt.setLong(5, batch.getBatchId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0)
                throw new DatabaseOperationException("Updating stock batch failed, no rows affected for ID: " + batch.getBatchId());
            return batch;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error updating stock batch: " + e.getMessage(), e);
        }
    }

    public void delete(StockBatch batch, Connection conn) throws DatabaseOperationException {
        String sql = "DELETE FROM stock_batches WHERE batch_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, batch.getBatchId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting stock batch: " + e.getMessage(), e);
        }
    }

    public List<StockBatch> findAll(Connection conn) throws DatabaseOperationException {
        String sql = "SELECT * FROM stock_batches";
        List<StockBatch> batches = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                batches.add(mapRowToStockBatch(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all stock batches: " + e.getMessage(), e);
        }
        return batches;
    }

    public List<StockBatch> findByItemForUpdate(Item item, Connection conn) throws DatabaseOperationException {
        String sql = "SELECT * FROM stock_batches WHERE item_code = ? FOR UPDATE";
        List<StockBatch> batches = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getItemCode());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) batches.add(mapRowToStockBatch(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding stock batches for update: " + e.getMessage(), e);
        }
        return batches;
    }

    @Override
    public List<StockBatch> findExpiredBatchesForUpdate(LocalDate currentDate, Connection conn) throws DatabaseOperationException {
        String sql = "SELECT * FROM stock_batches WHERE expiry_date <= ? FOR UPDATE";
        List<StockBatch> expiredBatches = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(currentDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expiredBatches.add(mapRowToStockBatch(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error fetching expired stock batches for update: " + e.getMessage(), e);
        }
        return expiredBatches;
    }
}
