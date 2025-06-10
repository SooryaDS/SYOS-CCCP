package com.syos.adapter.out.DatabaseSpecificAdaptors.implementation;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.EmployeeRepository;
import com.syos.domain.model.Employee;
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
        String sql = "SELECT * FROM employees WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Employee employee = new Employee(
                            rs.getInt("employee_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            rs.getString("role")
                    );
                    return Optional.of(employee);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error finding employee by username: " + username, e);
        }
        return Optional.empty();
    }
}
