package com.syos.application.usecase;

import com.syos.application.port.AuthenticationService;
import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.model.Employee;
import com.syos.domain.model.OnlineUser;
import com.syos.domain.enums.UserRole;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.EmployeeRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineUserRepository;
import com.syos.adapter.out.util.PasswordUtil; // Ensure this import is correct
import java.util.Optional;

public class AuthenticationServiceImplementation implements AuthenticationService {

    private final OnlineUserRepository onlineUserRepository;
    private final EmployeeRepository employeeRepository;

    public AuthenticationServiceImplementation(OnlineUserRepository onlineUserRepository, EmployeeRepository employeeRepository) {
        this.onlineUserRepository = onlineUserRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Optional<AuthenticatedUser> authenticate(String username, String password) throws DatabaseOperationException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        // First, check if the user is an employee
        Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            // Password verification using the utility class
            if (PasswordUtil.verifyPassword(password, employee.getPasswordHash())) {
                // Note: Your app currently maps all authenticated employees to UserRole.STAFF.
                // If you need specific 'admin' role (which you likely do for an admin),
                // you would modify the AuthenticatedUser to store the actual role from the Employee entity
                // and use it here (e.g., employee.getRole() if it's available).
                return Optional.of(new AuthenticatedUser(employee.getEmployeeId(), employee.getUsername(), UserRole.STAFF));
            }
        }

        // If not an employee, check if they are an online customer
        Optional<OnlineUser> customerOpt = onlineUserRepository.findByUsername(username);
        if (customerOpt.isPresent()) {
            OnlineUser customer = customerOpt.get();
            // Password verification using the utility class
            if (PasswordUtil.verifyPassword(password, customer.getPasswordHash())) {
                return Optional.of(new AuthenticatedUser(customer.getUserId(), customer.getUsername(), UserRole.CUSTOMER));
            }
        }

        return Optional.empty();
    }
}
