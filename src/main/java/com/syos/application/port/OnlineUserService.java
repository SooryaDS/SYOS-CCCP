package com.syos.application.port;

import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;

import java.util.Optional; // New import

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
}