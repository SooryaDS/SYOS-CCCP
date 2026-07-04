package com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.ItemNotFoundException;
import com.syos.domain.exception.InsufficientStockException;

import com.syos.domain.model.Item;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.sql.*;

public class ItemRepositoryImpl implements ItemRepository {

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        return new Item(
                rs.getString("item_code"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getBigDecimal("unit_price"),
                rs.getInt("reorder_level"),
                rs.getInt("reorder_quantity")
        );
    }

    @Override
    public Item save(Item item) throws DatabaseOperationException {
        String sql = """
            INSERT INTO items (item_code, name, description, category, unit_price, reorder_level, reorder_quantity)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              name = VALUES(name),
              description = VALUES(description),
              category = VALUES(category),
              unit_price = VALUES(unit_price),
              reorder_level = VALUES(reorder_level),
              reorder_quantity = VALUES(reorder_quantity)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getItemCode());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getCategory());
            stmt.setBigDecimal(5, item.getUnitPrice());
            stmt.setInt(6, item.getReorderLevel());
            stmt.setInt(7, item.getReorderQuantity());

            stmt.executeUpdate();
            return item;

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving item", e);
        }
    }

    @Override
    public Optional<Item> findByItemCode(String itemCode) throws DatabaseOperationException {
        String sql = "SELECT * FROM items WHERE item_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding item", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Item> findByItemCodeForUpdate(String itemCode, Connection conn)
            throws DatabaseOperationException {

        String sql = "SELECT * FROM items WHERE item_code = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding item for update", e);
        }
        return Optional.empty();
    }

    @Override
    public Item update(Item item) throws DatabaseOperationException {

        String sql = """
            UPDATE items SET
                name = ?, description = ?, category = ?, unit_price = ?,
                reorder_level = ?, reorder_quantity = ?
            WHERE item_code = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setString(3, item.getCategory());
            stmt.setBigDecimal(4, item.getUnitPrice());
            stmt.setInt(5, item.getReorderLevel());
            stmt.setInt(6, item.getReorderQuantity());
            stmt.setString(7, item.getItemCode());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new ItemNotFoundException("Item not found: " + item.getItemCode());
            }
            return item;

        } catch (SQLException | ItemNotFoundException e) {
            throw new DatabaseOperationException("Error updating item", e);
        }
    }

    // 1️⃣ Method with Connection (used internally in transactions)
    /**
     * Delete an item by its itemCode.
     * Deletes from child tables first, then the items table.
     * Handles transaction internally.
     */
// Transaction-safe method with Connection
    @Override
    public void deleteByItemCode(String itemCode, Connection conn)
            throws DatabaseOperationException, ItemNotFoundException {

        String deleteShelf   = "DELETE FROM shelf_stock WHERE item_code = ?";
        String deleteWebsite = "DELETE FROM website_stock WHERE item_code = ?";
        String deleteBatch   = "DELETE FROM stock_batches WHERE item_code = ?";
        String deleteItem    = "DELETE FROM items WHERE item_code = ?";

        try (
                PreparedStatement psShelf   = conn.prepareStatement(deleteShelf);
                PreparedStatement psWebsite = conn.prepareStatement(deleteWebsite);
                PreparedStatement psBatch   = conn.prepareStatement(deleteBatch);
                PreparedStatement psItem    = conn.prepareStatement(deleteItem)
        ) {
            psShelf.setString(1, itemCode);
            psShelf.executeUpdate();

            psWebsite.setString(1, itemCode);
            psWebsite.executeUpdate();

            psBatch.setString(1, itemCode);
            psBatch.executeUpdate();

            psItem.setString(1, itemCode);
            int affected = psItem.executeUpdate();

            if (affected == 0) {
                throw new ItemNotFoundException("Item not found: " + itemCode);
            }

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting item: " + e.getMessage(), e);
        }
    }

    // Simple version: manages its own Connection
    @Override
    public void deleteByItemCode(String itemCode) throws DatabaseOperationException, ItemNotFoundException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                deleteByItemCode(itemCode, conn); // call the transaction-safe method
                conn.commit();
            } catch (DatabaseOperationException | ItemNotFoundException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new DatabaseOperationException("Unexpected error: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database connection error: " + e.getMessage(), e);
        }
    }


    @Override
    public List<Item> findAll() throws DatabaseOperationException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapRowToItem(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving items", e);
        }
        return items;
    }

}
