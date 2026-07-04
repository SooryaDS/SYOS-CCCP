package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;

import java.util.List; // Added import for List
import java.util.Optional;

/**
 * Repository interface for managing OnlineUser data.
 * Defines the contract for online user data access, abstracting specific database operations.
 */
public interface OnlineUserRepository {
    /**
     * Saves a new online user to the database.
     * @param user The OnlineUser object to save.
     * @return The saved OnlineUser, potentially with a database-generated userId.
     * @throws DatabaseOperationException if a database error occurs, or if username/email already exists (handled by DB constraints).
     */
    OnlineUser save(OnlineUser user) throws DatabaseOperationException;

    /**
     * Updates an existing online user in the database.
     * @param user The OnlineUser object with updated details. The user must have a valid userId.
     * @return The updated OnlineUser object.
     * @throws DatabaseOperationException if a database error occurs (e.g., user not found, or constraint violation).
     */
    OnlineUser update(OnlineUser user) throws DatabaseOperationException; // ADDED: Update method

    /**
     * Deletes an online user from the database by their ID.
     * @param userId The ID of the online user to delete.
     * @throws DatabaseOperationException if a database error occurs (e.g., user not found, or foreign key constraints).
     */
    void delete(int userId) throws DatabaseOperationException; // ADDED: Delete method

    /**
     * Finds an online user by their unique user ID.
     * This method is needed by OnlineOrderingService to retrieve user address.
     * @param userId The ID of the online user to search for.
     * @return An Optional containing the OnlineUser if found, or empty if not found.
     * @throws DatabaseOperationException if a database error occurs.
     */
    Optional<OnlineUser> findById(int userId) throws DatabaseOperationException;

    /**
     * Finds an online user by their username.
     * @param username The username to search for.
     * @return An Optional containing the OnlineUser if found.
     * @throws DatabaseOperationException if a database error occurs.
     */
    Optional<OnlineUser> findByUsername(String username) throws DatabaseOperationException;

    /**
     * Finds an online user by their email.
     * @param email The email to search for.
     * @return An Optional containing the OnlineUser if found.
     * @throws DatabaseOperationException if a database error occurs.
     */
    Optional<OnlineUser> findByEmail(String email) throws DatabaseOperationException;

    /**
     * Retrieves all online users from the database.
     * @return A list of all OnlineUser objects.
     * @throws DatabaseOperationException if a database error occurs.
     */
    List<OnlineUser> findAll() throws DatabaseOperationException; // ADDED: findAll method
}
