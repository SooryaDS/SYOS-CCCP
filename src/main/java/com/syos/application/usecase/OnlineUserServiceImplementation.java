package com.syos.application.usecase;

import com.syos.application.port.OnlineUserService;
import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;
import com.syos.domain.exception.UserNotFoundException; // New import
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineUserRepository;
import com.syos.adapter.out.util.PasswordUtil;

import java.util.List; // New import
import java.util.Optional;

public class OnlineUserServiceImplementation implements OnlineUserService {

    private final OnlineUserRepository onlineUserRepository;

    public OnlineUserServiceImplementation(OnlineUserRepository onlineUserRepository) {
        this.onlineUserRepository = onlineUserRepository;
    }

    @Override
    public OnlineUser registerUser(String username, String password, String fullName, String email, String address)
            throws UserAlreadyExistsException, DatabaseOperationException, IllegalArgumentException {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required.");
        }

        if (onlineUserRepository.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("Username '" + username + "' is already taken.");
        }
        if (onlineUserRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("Email '" + email + "' is already registered.");
        }

        String hashedPassword = PasswordUtil.hashPassword(password);

        OnlineUser newUser = new OnlineUser(username, hashedPassword, fullName, email, address);

        try {
            return onlineUserRepository.save(newUser);
        } catch (DatabaseOperationException e) {
            // This block handles potential database constraint violations
            if (e.getMessage().toLowerCase().contains("duplicate entry") || e.getMessage().toLowerCase().contains("already exists")) {
                // Re-check after DB error to determine if it was username or email conflict
                if (onlineUserRepository.findByUsername(username).isPresent()) {
                    throw new UserAlreadyExistsException("Username '" + username + "' is already taken (DB constraint).", e);
                }
                if (onlineUserRepository.findByEmail(email).isPresent()) {
                    throw new UserAlreadyExistsException("Email '" + email + "' is already registered (DB constraint).", e);
                }
            }
            throw e; // Re-throw if it's another type of DB error
        }
    }

    @Override
    public Optional<OnlineUser> authenticateUser(String username, String plainPassword) throws DatabaseOperationException {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return Optional.empty(); // Basic validation
        }
        Optional<OnlineUser> userOptional = onlineUserRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            OnlineUser user = userOptional.get();
            if (PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }
        return Optional.empty(); // Authentication failed
    }

    @Override
    public OnlineUser updateUser(OnlineUser user) throws DatabaseOperationException, UserNotFoundException, IllegalArgumentException {
        if (user == null) {
            throw new IllegalArgumentException("User to update cannot be null.");
        }
        if (user.getUserId() <= 0) {
            throw new IllegalArgumentException("User ID must be positive for update.");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty() || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        // 1. Check if user exists by ID
        Optional<OnlineUser> existingUserById = onlineUserRepository.findById(user.getUserId());
        if (existingUserById.isEmpty()) {
            throw new UserNotFoundException("User with ID " + user.getUserId() + " not found for update.");
        }

        // 2. Check for username conflicts
        Optional<OnlineUser> existingUserWithSameUsername = onlineUserRepository.findByUsername(user.getUsername());
        if (existingUserWithSameUsername.isPresent() && existingUserWithSameUsername.get().getUserId() != user.getUserId()) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' is already taken by another user.");
        }

        // 3. Check for email conflicts
        Optional<OnlineUser> existingUserWithSameEmail = onlineUserRepository.findByEmail(user.getEmail());
        if (existingUserWithSameEmail.isPresent() && existingUserWithSameEmail.get().getUserId() != user.getUserId()) {
            throw new IllegalArgumentException("Email '" + user.getEmail() + "' is already registered by another user.");
        }

        // If a new password is provided (and it's not already hashed), hash it
        // This logic assumes `user.getPasswordHash()` would contain the new plaintext password if it's being updated.
        // A more robust solution might have a separate `updatePassword` method or check if `user.getPasswordHash()`
        // has changed from the existing user's hashed password.
        if (user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty()) {
            // Check if it's already a hashed password (e.g., starts with $2a$ or $2b$)
            if (!user.getPasswordHash().startsWith("$2a$") && !user.getPasswordHash().startsWith("$2b$")) {
                String newHashedPassword = PasswordUtil.hashPassword(user.getPasswordHash());
                user.setPasswordHash(newHashedPassword);
            }
        } else {
            // If password is empty or null in the 'user' object, retain the old password from the existing user
            user.setPasswordHash(existingUserById.get().getPasswordHash());
        }

        try {
            return onlineUserRepository.update(user);
        } catch (DatabaseOperationException e) {
            // Re-check for duplicate entries if database constraint causes error during update
            if (e.getMessage().toLowerCase().contains("duplicate entry") || e.getMessage().toLowerCase().contains("already exists")) {
                if (onlineUserRepository.findByUsername(user.getUsername()).isPresent() && onlineUserRepository.findByUsername(user.getUsername()).get().getUserId() != user.getUserId()) {
                    throw new IllegalArgumentException("Username '" + user.getUsername() + "' is already taken by another user (DB constraint).", e);
                }
                if (onlineUserRepository.findByEmail(user.getEmail()).isPresent() && onlineUserRepository.findByEmail(user.getEmail()).get().getUserId() != user.getUserId()) {
                    throw new IllegalArgumentException("Email '" + user.getEmail() + "' is already registered by another user (DB constraint).", e);
                }
            }
            throw e;
        }
    }

    @Override
    public void deleteUser(int userId) throws DatabaseOperationException, UserNotFoundException, IllegalArgumentException {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive for deletion.");
        }
        if (onlineUserRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException("User with ID " + userId + " not found for deletion.");
        }
        onlineUserRepository.delete(userId);
    }

    @Override
    public Optional<OnlineUser> getUserById(int userId) throws DatabaseOperationException, IllegalArgumentException {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive for retrieval.");
        }
        return onlineUserRepository.findById(userId);
    }

    @Override
    public Optional<OnlineUser> getUserByUsername(String username) throws DatabaseOperationException, IllegalArgumentException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username for retrieval cannot be null or empty.");
        }
        return onlineUserRepository.findByUsername(username);
    }

    @Override
    public List<OnlineUser> getAllUsers() throws DatabaseOperationException {
        return onlineUserRepository.findAll();
    }
}
