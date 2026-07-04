package com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineUserRepository;
import com.syos.domain.model.OnlineUser;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList; // Added import for ArrayList
import java.util.List;     // Added import for List
import java.util.Optional;

/**
 * JDBC implementation of the OnlineUserRepository interface.
 * Handles persistence operations for OnlineUser objects in a MySQL database.
 */
public class OnlineUserRepositoryImpl implements OnlineUserRepository {

    /**
     * Saves a new online user to the database.
     * @param user The OnlineUser object to save.
     * @return The saved OnlineUser, with its auto-generated userId updated.
     * @throws DatabaseOperationException if a database error occurs, or if username/email already exists.
     */
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
            if (e.getErrorCode() == 1062) {
                if (e.getMessage().toLowerCase().contains("username")) {
                    throw new DatabaseOperationException("Username '" + user.getUsername() + "' already exists.", e);
                } else if (e.getMessage().toLowerCase().contains("email")) {
                    throw new DatabaseOperationException("Email '" + user.getEmail() + "' already exists.", e);
                }
            }
            throw new DatabaseOperationException("Error saving online user: " + user.getUsername(), e);
        }
    }

    /**
     * Updates an existing online user in the database.
     * @param user The OnlineUser object with updated details. The user must have a valid userId.
     * @return The updated OnlineUser object.
     * @throws DatabaseOperationException if a database error occurs (e.g., user not found, or constraint violation).
     */
    @Override
    public OnlineUser update(OnlineUser user) throws DatabaseOperationException {
        String sql = "UPDATE online_users SET username = ?, password_hash = ?, full_name = ?, email = ?, address = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getAddress());
            pstmt.setInt(6, user.getUserId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Updating user failed, no user found with ID: " + user.getUserId());
            }
            return user;
        } catch (SQLException e) {
            // Check for unique constraint violations (MySQL error code 1062)
            if (e.getErrorCode() == 1062) {
                if (e.getMessage().toLowerCase().contains("username")) {
                    throw new DatabaseOperationException("Username '" + user.getUsername() + "' already exists during update.", e);
                } else if (e.getMessage().toLowerCase().contains("email")) {
                    throw new DatabaseOperationException("Email '" + user.getEmail() + "' already exists during update.", e);
                }
            }
            throw new DatabaseOperationException("Error updating online user with ID: " + user.getUserId() + ". " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an online user from the database by their ID.
     * @param userId The ID of the online user to delete.
     * @throws DatabaseOperationException if a database error occurs (e.g., user not found, or foreign key constraints).
     */
    @Override
    public void delete(int userId) throws DatabaseOperationException {
        String sql = "DELETE FROM online_users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Deleting user failed, no user found with ID: " + userId);
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error deleting online user with ID: " + userId + ". " + e.getMessage(), e);
        }
    }

    /**
     * Finds an online user by their unique user ID.
     * @param userId The ID of the online user to search for.
     * @return An Optional containing the OnlineUser if found, or empty if not found.
     * @throws DatabaseOperationException if a database error occurs.
     */
    @Override
    public Optional<OnlineUser> findById(int userId) throws DatabaseOperationException {
        String sql = "SELECT user_id, username, password_hash, full_name, email, registration_date, address " +
                "FROM online_users WHERE user_id = ?";
        OnlineUser user = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = mapResultSetToOnlineUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding online user by ID: " + userId + ". " + e.getMessage(), e);
        }
        return Optional.ofNullable(user);
    }

    /**
     * Finds an online user by their username.
     * @param username The username to search for.
     * @return An Optional containing the OnlineUser if found.
     * @throws DatabaseOperationException if a database error occurs.
     */
    @Override
    public Optional<OnlineUser> findByUsername(String username) throws DatabaseOperationException {
        String sql = "SELECT user_id, username, password_hash, full_name, email, registration_date, address " +
                "FROM online_users WHERE username = ?";
        OnlineUser user = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = mapResultSetToOnlineUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by username: " + username + ". " + e.getMessage(), e);
        }
        return Optional.ofNullable(user);
    }

    /**
     * Finds an online user by their email.
     * @param email The email to search for.
     * @return An Optional containing the OnlineUser if found.
     * @throws DatabaseOperationException if a database error occurs.
     */
    @Override
    public Optional<OnlineUser> findByEmail(String email) throws DatabaseOperationException {
        String sql = "SELECT user_id, username, password_hash, full_name, email, registration_date, address " +
                "FROM online_users WHERE email = ?";
        OnlineUser user = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = mapResultSetToOnlineUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding user by email: " + email + ". " + e.getMessage(), e);
        }
        return Optional.ofNullable(user);
    }

    /**
     * Retrieves all online users from the database.
     * @return A list of all OnlineUser objects.
     * @throws DatabaseOperationException if a database error occurs.
     */
    @Override
    public List<OnlineUser> findAll() throws DatabaseOperationException {
        List<OnlineUser> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash, full_name, email, registration_date, address FROM online_users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) { // ResultSet managed by try-with-resources

            while (rs.next()) {
                users.add(mapResultSetToOnlineUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error retrieving all online users: " + e.getMessage(), e);
        }
        return users;
    }

    /**
     * Helper method to map a ResultSet row to an OnlineUser object.
     * @param rs The ResultSet containing the user data.
     * @return An OnlineUser object.
     * @throws SQLException If a SQL error occurs during data retrieval from ResultSet.
     */
    private OnlineUser mapResultSetToOnlineUser(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        // Ensure registration_date is not null before converting
        Timestamp regTimestamp = rs.getTimestamp("registration_date");
        LocalDateTime registrationDate = (regTimestamp != null) ? regTimestamp.toLocalDateTime() : null; // Handle potential null date from DB
        String address = rs.getString("address");

        // Use the appropriate constructor. Assume address can be null/empty from DB if allowed.
        return new OnlineUser(userId, username, passwordHash, fullName, email, registrationDate, address);
    }
}
