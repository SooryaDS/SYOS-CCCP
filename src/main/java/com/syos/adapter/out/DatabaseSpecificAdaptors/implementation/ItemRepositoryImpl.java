package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.ItemRepository;
import com.syos.domain.model.Item;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemRepositoryImpl implements ItemRepository {

    @Override
    public Optional<Item> findByItemCode(String itemCode) throws DatabaseOperationException {
        // SQL to select an item by its code, NOW INCLUDING min_shelf_stock_threshold
        String sql = "SELECT item_code, name, description, category, unit_price, reorder_level_threshold, min_shelf_stock_threshold FROM items WHERE item_code = ?";
        Item item = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Create an Item object from the ResultSet, NOW INCLUDING min_shelf_stock_threshold
                item = new Item(
                        rs.getString("item_code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("reorder_level_threshold"),
                        rs.getInt("min_shelf_stock_threshold") // NEW: Pass the value from ResultSet
                );
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding item by code: " + itemCode, e);
        }
        return Optional.ofNullable(item);
    }

    @Override
    public List<Item> findAll() throws DatabaseOperationException {
        List<Item> items = new ArrayList<>();
        // SQL NOW INCLUDING min_shelf_stock_threshold
        String sql = "SELECT item_code, name, description, category, unit_price, reorder_level_threshold, min_shelf_stock_threshold FROM items";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                items.add(new Item(
                        rs.getString("item_code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("reorder_level_threshold"),
                        rs.getInt("min_shelf_stock_threshold") // NEW: Pass the value from ResultSet
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all items", e);
        }
        return items;
    }


    @Override
    public Item save(Item item) throws DatabaseOperationException {
        // SQL for INSERT ... ON DUPLICATE KEY UPDATE, NOW INCLUDING min_shelf_stock_threshold
        String sql = "INSERT INTO items (item_code, name, description, category, unit_price, reorder_level_threshold, min_shelf_stock_threshold) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " + // Added one '?'
                "ON DUPLICATE KEY UPDATE " +
                "name = VALUES(name), description = VALUES(description), category = VALUES(category), " +
                "unit_price = VALUES(unit_price), reorder_level_threshold = VALUES(reorder_level_threshold), " +
                "min_shelf_stock_threshold = VALUES(min_shelf_stock_threshold)"; // NEW: Add this column update

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getItemCode());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getCategory());
            pstmt.setBigDecimal(5, item.getUnitPrice());
            pstmt.setInt(6, item.getReorderLevelThreshold());
            pstmt.setInt(7, item.getMinShelfStockThreshold()); // NEW: Set the value for the new column

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("Warning: Save operation affected 0 rows for item: " + item.getItemCode());
            }
            return item;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error saving item: " + item.getItemCode(), e);
        }
    }
}