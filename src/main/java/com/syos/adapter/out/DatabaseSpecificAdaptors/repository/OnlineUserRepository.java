package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;

import java.util.Optional;

public interface OnlineUserRepository {
    /**
     * Saves a new online user to the database.
     * @param user The OnlineUser object to save.
     * @return The saved OnlineUser, potentially with a database-generated userId.
     * @throws DatabaseOperationException if a database error occurs, or if username/email already exists (handled by DB constraints).
     */
    OnlineUser save(OnlineUser user) throws DatabaseOperationException;

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

    // Optional<OnlineUser> findById(int userId) throws DatabaseOperationException; // If needed later
}