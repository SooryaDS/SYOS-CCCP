package com.syos.application.usecase;

import com.syos.application.port.AuthenticationService;
import com.syos.domain.model.AuthenticatedUser;
import com.syos.domain.model.Employee;
import com.syos.domain.model.OnlineUser;
import com.syos.domain.enums.UserRole;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.EmployeeRepository;
import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineUserRepository;
import com.syos.adapter.out.util.PasswordUtil;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Auth service with async support
 * Design: Strategy Pattern (interface + implementation)
 */
public class AuthenticationServiceImplementation implements AuthenticationService {

    private final OnlineUserRepository onlineRepo;
    private final EmployeeRepository employeeRepo;
    private final ExecutorService executor;

    public AuthenticationServiceImplementation(OnlineUserRepository onlineRepo, EmployeeRepository employeeRepo) {
        this.onlineRepo = onlineRepo;
        this.employeeRepo = employeeRepo;
        int threads = Runtime.getRuntime().availableProcessors() * 4; // tune for load
        this.executor = Executors.newFixedThreadPool(threads);
    }

    /** Async login */
    public CompletableFuture<Optional<AuthenticatedUser>> authenticateAsync(String user, String pass) {
        return CompletableFuture.supplyAsync(() -> {
            try { return authenticate(user, pass); }
            catch (DatabaseOperationException e) { e.printStackTrace(); return Optional.empty(); }
        }, executor);
    }

    /** Sync login */
    @Override
    public Optional<AuthenticatedUser> authenticate(String user, String pass) throws DatabaseOperationException {
        if (user == null || user.isBlank() || pass == null || pass.isEmpty()) return Optional.empty();

        Optional<Employee> empOpt = employeeRepo.findByUsername(user);
        if (empOpt.isPresent() && PasswordUtil.verifyPassword(pass, empOpt.get().getPasswordHash())) {
            return Optional.of(new AuthenticatedUser(empOpt.get().getEmployeeId(), user, UserRole.STAFF));
        }

        Optional<OnlineUser> custOpt = onlineRepo.findByUsername(user);
        if (custOpt.isPresent() && PasswordUtil.verifyPassword(pass, custOpt.get().getPasswordHash())) {
            return Optional.of(new AuthenticatedUser(custOpt.get().getUserId(), user, UserRole.CUSTOMER));
        }

        return Optional.empty();
    }

    /** Stop thread pool */
    public void shutdown() { executor.shutdown(); }
}
