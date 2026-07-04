package com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ShelfStockRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.model.ShelfStock;
import com.syos.domain.model.Item;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShelfStockRepositoryImpl implements ShelfStockRepository {

    private final ItemRepository itemRepository;

    public ShelfStockRepositoryImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // --- Helper method to map ResultSet row to ShelfStock ---
    private ShelfStock mapRowToShelfStock(ResultSet rs) throws SQLException, DatabaseOperationException {
        Long shelfStockId = rs.getLong("shelf_stock_id");
        String itemCode = rs.getString("item_code");
        int quantityOnShelf = rs.getInt("quantity_on_shelf");
        Timestamp lastUpdatedTimestamp = rs.getTimestamp("last_updated_date");
        LocalDateTime lastUpdatedDate = lastUpdatedTimestamp != null ? lastUpdatedTimestamp.toLocalDateTime() : null;

        Item item = itemRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new DatabaseOperationException(
                        "Referenced Item with code " + itemCode + " not found for ShelfStock ID " + shelfStockId
                ));

        return new ShelfStock(shelfStockId, item, quantityOnShelf, lastUpdatedDate);
    }

    // --- Private method to handle insert/update logic ---
    private ShelfStock saveOrUpdate(ShelfStock shelfStock, Connection existingConn) throws DatabaseOperationException {
        boolean closeConn = false;
        try {
            Connection conn = existingConn != null ? existingConn : DatabaseConnection.getConnection();
            closeConn = existingConn == null;

            if (shelfStock.getShelfStockId() == null || shelfStock.getShelfStockId() == 0) {
                // INSERT
                String insertSql = "INSERT INTO shelf_stock (item_code, quantity_on_shelf, last_updated_date) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, shelfStock.getItem().getItemCode());
                    stmt.setInt(2, shelfStock.getQuantityOnShelf());
                    stmt.setTimestamp(3, Timestamp.valueOf(shelfStock.getLastUpdatedDate()));
                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows == 0) throw new DatabaseOperationException("Creating shelf stock failed, no rows affected.");

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            shelfStock.setShelfStockId(generatedKeys.getLong(1));
                        } else {
                            throw new DatabaseOperationException("Creating shelf stock failed, no ID obtained.");
                        }
                    }
                }
            } else {
                // UPDATE
                String updateSql = "UPDATE shelf_stock SET quantity_on_shelf = ?, last_updated_date = ? WHERE shelf_stock_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, shelfStock.getQuantityOnShelf());
                    stmt.setTimestamp(2, Timestamp.valueOf(shelfStock.getLastUpdatedDate()));
                    stmt.setLong(3, shelfStock.getShelfStockId());
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        System.err.println("Warning: Updating shelf stock failed, no rows affected for ID: " + shelfStock.getShelfStockId());
                    }
                }
            }
            return shelfStock;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving/updating shelf stock: " + e.getMessage(), e);
        }
    }

    // --- Private helper to delete by ID ---
    private void deleteByIdInternal(Long id, Connection existingConn) throws DatabaseOperationException {
        boolean closeConn = false;
        try {
            Connection conn = existingConn != null ? existingConn : DatabaseConnection.getConnection();
            closeConn = existingConn == null;

            String sql = "DELETE FROM shelf_stock WHERE shelf_stock_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting shelf stock by ID: " + e.getMessage(), e);
        }
    }

    // --- Public CRUD implementations ---

    @Override
    public ShelfStock save(ShelfStock shelfStock) throws DatabaseOperationException {
        return saveOrUpdate(shelfStock, null);
    }

    @Override
    public ShelfStock save(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException {
        return saveOrUpdate(shelfStock, conn);
    }

    @Override
    public Optional<ShelfStock> findById(Long id) throws DatabaseOperationException {
        String sql = "SELECT * FROM shelf_stock WHERE shelf_stock_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToShelfStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding shelf stock by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ShelfStock> findByItem(Item item) throws DatabaseOperationException {
        String sql = "SELECT * FROM shelf_stock WHERE item_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getItemCode());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToShelfStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding shelf stock by item: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ShelfStock> findByItemCodeAndBatchId(String itemCode, int batchId) throws DatabaseOperationException {
        String sql = "SELECT * FROM shelf_stock WHERE item_code = ? AND batch_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemCode);
            stmt.setInt(2, batchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToShelfStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding shelf stock by item code and batch ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<ShelfStock> findAll() throws DatabaseOperationException {
        String sql = "SELECT * FROM shelf_stock";
        List<ShelfStock> shelfStocks = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) shelfStocks.add(mapRowToShelfStock(rs));
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all shelf stock: " + e.getMessage(), e);
        }
        return shelfStocks;
    }

    @Override
    public int getAvailableQuantity(String itemCode) throws DatabaseOperationException {
        String sql = "SELECT COALESCE(SUM(quantity_on_shelf), 0) FROM shelf_stock WHERE item_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting available quantity: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public ShelfStock update(ShelfStock shelfStock) throws DatabaseOperationException {
        return save(shelfStock);
    }

    @Override
    public ShelfStock update(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException {
        return save(shelfStock, conn);
    }

    @Override
    public void delete(Long id) throws DatabaseOperationException {
        deleteByIdInternal(id, null);
    }

    @Override
    public void delete(ShelfStock shelfStock) throws DatabaseOperationException {
        if (shelfStock == null || shelfStock.getShelfStockId() == null)
            throw new IllegalArgumentException("ShelfStock or its ID cannot be null for deletion.");
        deleteByIdInternal(shelfStock.getShelfStockId(), null);
    }

    @Override
    public void delete(ShelfStock shelfStock, Connection conn) throws DatabaseOperationException {
        if (shelfStock == null || shelfStock.getShelfStockId() == null)
            throw new IllegalArgumentException("ShelfStock or its ID cannot be null for deletion.");
        deleteByIdInternal(shelfStock.getShelfStockId(), conn);
    }

    @Override
    public void deleteById(Long id) throws DatabaseOperationException {
        deleteByIdInternal(id, null);
    }

    @Override
    public Optional<ShelfStock> findByItemForUpdate(Item item, Connection conn) throws DatabaseOperationException {
        String sql = "SELECT * FROM shelf_stock WHERE item_code = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getItemCode());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToShelfStock(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding shelf stock for update: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

}
