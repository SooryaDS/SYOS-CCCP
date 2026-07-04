package com.syos.adapter.out.DatabaseSpecificAdaptors.jdbcimplementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.EmployeeRepository;
import com.syos.domain.model.Employee;
import com.syos.domain.enums.UserRole;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class EmployeeRepositoryImpl implements EmployeeRepository {

    @Override
    public Optional<Employee> findByUsername(String username) throws DatabaseOperationException {
        String sql = "SELECT employee_id, username, password_hash, full_name, role FROM employees WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // FIX: Convert the String role from the database to a UserRole enum
                    UserRole roleEnum = UserRole.valueOf(rs.getString("role").toUpperCase()); // Ensure case matches enum

                    Employee employee = new Employee(
                            rs.getInt("employee_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            roleEnum // Pass the converted enum
                    );
                    return Optional.of(employee);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding employee by username: " + username, e);
        } catch (IllegalArgumentException e) {
            // Catch if the role string from DB doesn't match any UserRole enum constant
            throw new DatabaseOperationException("Invalid role found in database for employee " + username + ": " + e.getMessage(), e);
        }
        return Optional.empty();
    }
}