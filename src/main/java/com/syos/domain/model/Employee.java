package com.syos.domain.model;

import java.util.Objects;

public class Employee {
    private int employeeId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role;

    public Employee(String username, String passwordHash, String fullName, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public Employee(int employeeId, String username, String passwordHash, String fullName, String role) {
        this(username, passwordHash, fullName, role);
        this.employeeId = employeeId;
    }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return employeeId == employee.employeeId && Objects.equals(username, employee.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, username);
    }
}