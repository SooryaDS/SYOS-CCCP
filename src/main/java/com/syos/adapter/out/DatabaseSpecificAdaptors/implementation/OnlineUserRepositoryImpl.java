package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineUserRepository;
import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.util.Optional;

public class OnlineUserRepositoryImpl implements OnlineUserRepository {

    @Override
    public OnlineUser save(OnlineUser user) throws DatabaseOperationException {
        String sql = "INSERT INTO online_users (username, password_hash, full_name, email, registration_date, address) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getEmail());
            pstmt.setTimestamp(5, Timestamp.valueOf(user.getRegistrationDate()));
            pstmt.setString(6, user.getAddress());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1)); // Set the auto-generated userId
                } else {
                    throw new DatabaseOperationException("Creating user failed, no ID obtained.");
                }
            }
            return user;
        } catch (SQLException e) {
            // Check for unique constraint violations (MySQL error code 1062)
            if (e.getErrorCode() == 1062) { // 1062 is the MySQL error code for duplicate entry
                if (e.getMessage().toLowerCase().contains("username")) {
                    throw new DatabaseOperationException("Username '" + user.getUsername() + "' already exists.", e);
                } else if (e.getMessage().toLowerCase().contains("email")) {
                    throw new DatabaseOperationException("Email '" + user.getEmail() + "' already exists.", e);
                }
            }
            throw new DatabaseOperationException("Error saving online user: " + user.getUsername(), e);
        }
    }

    @Override
    public Optional<OnlineUser> findByUsername(String username) throws DatabaseOperationException {
        String sql = "SELECT user_id, username, password_hash, full_name, email, registration_date, address " +
                "FROM online_users WHERE username = ?";
        OnlineUser user = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                user = new OnlineUser(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getTimestamp("registration_date").toLocalDateTime(),
                        rs.getString("address")
                );
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by username: " + username, e);
        }
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<OnlineUser> findByEmail(String email) throws DatabaseOperationException {
        String sql = "SELECT user_id, username, password_hash, full_name, email, registration_date, address " +
                "FROM online_users WHERE email = ?";
        OnlineUser user = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                user = new OnlineUser(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getTimestamp("registration_date").toLocalDateTime(),
                        rs.getString("address")
                );
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by email: " + email, e);
        }
        return Optional.ofNullable(user);
    }
}