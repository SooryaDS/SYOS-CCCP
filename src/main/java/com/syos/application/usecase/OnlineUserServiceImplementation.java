package com.syos.application.usecase;

import com.syos.application.port.OnlineUserService;
import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineUserRepository;
import com.syos.adapter.out.util.PasswordUtil;

import java.util.Optional; // New import

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
            if (e.getMessage().toLowerCase().contains("duplicate entry") || e.getMessage().toLowerCase().contains("already exists")) {
                if (onlineUserRepository.findByUsername(username).isPresent()) {
                    throw new UserAlreadyExistsException("Username '" + username + "' is already taken (DB constraint).");
                }
                if (onlineUserRepository.findByEmail(email).isPresent()) {
                    throw new UserAlreadyExistsException("Email '" + email + "' is already registered (DB constraint).");
                }
            }
            throw e;
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
}