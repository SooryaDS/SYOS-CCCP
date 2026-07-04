package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.ItemNotFoundException;

import com.syos.domain.model.Item;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;


public interface ItemRepository {
    /**
     * Saves a new Item to the database.
     * @param item The Item object to save.
     * @return The saved Item object, with its database-generated ID (if applicable).
     * @throws DatabaseOperationException If a database error occurs.
     */
    Item save(Item item) throws DatabaseOperationException;

    /**
     * Finds an Item by its unique item code.
     * @param itemCode The code of the item to search for.
     * @return An Optional containing the Item if found, empty otherwise.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<Item> findByItemCode(String itemCode) throws DatabaseOperationException;

    /**
     * Finds an Item by its unique item code using a database row-level lock.
     * @param itemCode The code of the item to search for.
     * @param conn The JDBC Connection to use for the transaction.
     * @return An Optional containing the Item if found, empty otherwise.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<Item> findByItemCodeForUpdate(String itemCode, Connection conn) throws DatabaseOperationException;

    /**
     * Updates an existing Item in the database.
     * @param item The Item object with updated details.
     * @return The updated Item object.
     * @throws DatabaseOperationException If a database error occurs or no record found.
     */
    Item update(Item item) throws DatabaseOperationException;

    /**
     * Deletes an Item from the database by its item code (standalone, manages connection internally).
     * @param itemCode The code of the item to delete.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws ItemNotFoundException If the item does not exist.
     */

    /**
     * Retrieves all Item entries from the database.
     * @return A list of all Item objects.
     * @throws DatabaseOperationException If a database error occurs.
     */
    List<Item> findAll() throws DatabaseOperationException;

    void deleteByItemCode(String itemCode) throws DatabaseOperationException, ItemNotFoundException;

    /**
     * Deletes an Item from the database by its item code using an existing connection (transaction-safe).
     * @param itemCode The code of the item to delete.
     * @param conn The JDBC Connection to use.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws ItemNotFoundException If the item does not exist.
     */
    void deleteByItemCode(String itemCode, Connection conn) throws DatabaseOperationException, ItemNotFoundException;

}
