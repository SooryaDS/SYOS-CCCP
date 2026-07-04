package com.syos.application.port;

import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;
import com.syos.domain.exception.UserNotFoundException; // New import needed for update/delete/get methods

import java.util.List; // New import
import java.util.Optional;

public interface OnlineUserService {
    OnlineUser registerUser(String username, String password, String fullName, String email, String address)
            throws UserAlreadyExistsException, DatabaseOperationException, IllegalArgumentException;

    /**
     * Authenticates an online user.
     * @param username The username.
     * @param plainPassword The plain text password.
     * @return An Optional containing the OnlineUser if authentication is successful, otherwise empty.
     * @throws DatabaseOperationException If a database error occurs.
     */
    Optional<OnlineUser> authenticateUser(String username, String plainPassword) throws DatabaseOperationException;

    /**
     * Updates an existing online user's details.
     * @param user The OnlineUser object with updated details.
     * @return The updated OnlineUser object.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws UserNotFoundException If the user with the given ID is not found.
     * @throws IllegalArgumentException If the provided user data is invalid (e.g., conflicting username/email).
     */
    OnlineUser updateUser(OnlineUser user) throws DatabaseOperationException, UserNotFoundException, IllegalArgumentException;

    /**
     * Deletes an online user by their ID.
     * @param userId The ID of the user to delete.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws UserNotFoundException If the user with the given ID is not found.
     * @throws IllegalArgumentException If the user ID is invalid (e.g., non-positive).
     */
    void deleteUser(int userId) throws DatabaseOperationException, UserNotFoundException, IllegalArgumentException;

    /**
     * Retrieves an online user by their ID.
     * @param userId The ID of the user.
     * @return An Optional containing the OnlineUser if found, otherwise empty.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws IllegalArgumentException If the user ID is invalid (e.g., non-positive).
     */
    Optional<OnlineUser> getUserById(int userId) throws DatabaseOperationException, IllegalArgumentException;

    /**
     * Retrieves an online user by their username.
     * @param username The username of the user.
     * @return An Optional containing the OnlineUser if found, otherwise empty.
     * @throws DatabaseOperationException If a database error occurs.
     * @throws IllegalArgumentException If the username is null or empty.
     */
    Optional<OnlineUser> getUserByUsername(String username) throws DatabaseOperationException, IllegalArgumentException;

    /**
     * Retrieves a list of all online users.
     * @return A list of all OnlineUser objects.
     * @throws DatabaseOperationException If a database error occurs.
     */
    List<OnlineUser> getAllUsers() throws DatabaseOperationException;
}
