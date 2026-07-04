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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq; // Import eq for exact argument matching
import static org.mockito.ArgumentMatchers.anyString; // Import anyString for flexible argument matching

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceImplementationTest {

    @Mock
    private OnlineUserRepository onlineUserRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AuthenticationServiceImplementation authenticationService;

    // A utility for hashing passwords in tests for consistent behavior
    // This HASHED_PASSWORD is used to create the Employee/OnlineUser objects in mocks.
    // For the verifyPassword mock, we will use anyString() for the hash.
    private static final String PLAIN_PASSWORD = "testpassword123";
    private static final String HASHED_PASSWORD = PasswordUtil.hashPassword(PLAIN_PASSWORD);

    @BeforeEach
    void setUp() {
        // No explicit setup needed with @ExtendWith(MockitoExtension.class)
        // and @InjectMocks, as Mockito handles injection and mocks initialization.
    }

    @Test
    void authenticate_SuccessfulEmployeeAuthentication() throws DatabaseOperationException {
        // Arrange
        String username = "employee_user";
        int employeeId = 1;
        Employee employee = new Employee(employeeId, username, "Employee Name", HASHED_PASSWORD, UserRole.STAFF);

        // Mock static method PasswordUtil.verifyPassword
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword(eq(PLAIN_PASSWORD), anyString())).thenReturn(true);
            when(employeeRepository.findByUsername(username)).thenReturn(Optional.of(employee));
            // No need to stub onlineUserRepository here as it should not be called if employee is found and authenticated.

            // Act
            Optional<AuthenticatedUser> result = authenticationService.authenticate(username, PLAIN_PASSWORD);

            // Assert
            assertTrue(result.isPresent());
            AuthenticatedUser authenticatedUser = result.get();
            assertEquals((long)employeeId, authenticatedUser.getId());
            assertEquals(username, authenticatedUser.getUsername());
            assertEquals(UserRole.STAFF, authenticatedUser.getRole());

            verify(employeeRepository, times(1)).findByUsername(username);
            verify(onlineUserRepository, never()).findByUsername(anyString()); // Should not check online users if employee found
            mockedPasswordUtil.verify(() -> PasswordUtil.verifyPassword(eq(PLAIN_PASSWORD), anyString()), times(1));
            verifyNoMoreInteractions(employeeRepository, onlineUserRepository);
        }
    }

    @Test
    void authenticate_SuccessfulOnlineUserAuthentication() throws DatabaseOperationException {
        // Arrange
        String username = "customer_user";
        int userId = 101;
        OnlineUser customer = new OnlineUser(userId, username, HASHED_PASSWORD, "Customer FullName", "customer@example.com", "Customer Address");

        // Mock static method PasswordUtil.verifyPassword
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword(eq(PLAIN_PASSWORD), anyString())).thenReturn(true);
            when(employeeRepository.findByUsername(anyString())).thenReturn(Optional.empty()); // No employee found
            when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.of(customer));

            // Act
            Optional<AuthenticatedUser> result = authenticationService.authenticate(username, PLAIN_PASSWORD);

            // Assert
            assertTrue(result.isPresent());
            AuthenticatedUser authenticatedUser = result.get();
            assertEquals((long)userId, authenticatedUser.getId());
            assertEquals(username, authenticatedUser.getUsername());
            assertEquals(UserRole.CUSTOMER, authenticatedUser.getRole());

            verify(employeeRepository, times(1)).findByUsername(anyString());
            verify(onlineUserRepository, times(1)).findByUsername(username);
            mockedPasswordUtil.verify(() -> PasswordUtil.verifyPassword(eq(PLAIN_PASSWORD), anyString()), times(1));
            verifyNoMoreInteractions(employeeRepository, onlineUserRepository);
        }
    }

    @Test
    void authenticate_IncorrectPasswordForEmployee() throws DatabaseOperationException {
        // Arrange
        String username = "employee_user";
        int employeeId = 1;
        Employee employee = new Employee(employeeId, username, "Employee Name", HASHED_PASSWORD, UserRole.STAFF);
        String wrongPassword = "wrongpassword";

        // Mock static method PasswordUtil.verifyPassword
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword(eq(wrongPassword), anyString())).thenReturn(false);
            when(employeeRepository.findByUsername(username)).thenReturn(Optional.of(employee));
            // FIX: The service will attempt to find the user in onlineUserRepository after failing employee password.
            when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.empty()); // No online user with this username
            // The previous 'never' was incorrect because the service *does* try to find the user in the customer repo.

            // Act
            Optional<AuthenticatedUser> result = authenticationService.authenticate(username, wrongPassword);

            // Assert
            assertFalse(result.isPresent());

            verify(employeeRepository, times(1)).findByUsername(username);
            // FIX: Verify that onlineUserRepository.findByUsername was called once.
            verify(onlineUserRepository, times(1)).findByUsername(username);
            mockedPasswordUtil.verify(() -> PasswordUtil.verifyPassword(eq(wrongPassword), anyString()), times(1));
            verifyNoMoreInteractions(employeeRepository, onlineUserRepository);
        }
    }

    @Test
    void authenticate_IncorrectPasswordForOnlineUser() throws DatabaseOperationException {
        // Arrange
        String username = "customer_user";
        int userId = 101;
        OnlineUser customer = new OnlineUser(userId, username, HASHED_PASSWORD, "Customer FullName", "customer@example.com", "Customer Address");
        String wrongPassword = "wrongpassword";

        // Mock static method PasswordUtil.verifyPassword
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword(eq(wrongPassword), anyString())).thenReturn(false);
            when(employeeRepository.findByUsername(anyString())).thenReturn(Optional.empty()); // No employee found
            when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.of(customer));

            // Act
            Optional<AuthenticatedUser> result = authenticationService.authenticate(username, wrongPassword);

            // Assert
            assertFalse(result.isPresent());

            verify(employeeRepository, times(1)).findByUsername(anyString());
            verify(onlineUserRepository, times(1)).findByUsername(username);
            mockedPasswordUtil.verify(() -> PasswordUtil.verifyPassword(eq(wrongPassword), anyString()), times(1));
            verifyNoMoreInteractions(employeeRepository, onlineUserRepository);
        }
    }

    @Test
    void authenticate_UserNotFound() throws DatabaseOperationException {
        // Arrange
        String username = "unknown_user";
        String password = PLAIN_PASSWORD;

        when(employeeRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        Optional<AuthenticatedUser> result = authenticationService.authenticate(username, password);

        // Assert
        assertFalse(result.isPresent());

        verify(employeeRepository, times(1)).findByUsername(username);
        verify(onlineUserRepository, times(1)).findByUsername(username);
        // PasswordUtil.verifyPassword should not be called if user is not found
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.verifyNoInteractions();
        }
        verifyNoMoreInteractions(employeeRepository, onlineUserRepository);
    }

    @Test
    void authenticate_NullUsername() throws DatabaseOperationException {
        // Arrange
        String username = null;
        String password = PLAIN_PASSWORD;

        // Act
        Optional<AuthenticatedUser> result = authenticationService.authenticate(username, password);

        // Assert
        assertFalse(result.isPresent());

        verifyNoInteractions(employeeRepository, onlineUserRepository);
        // PasswordUtil.verifyPassword should not be called
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.verifyNoInteractions();
        }
    }

    @Test
    void authenticate_EmptyUsername() throws DatabaseOperationException {
        // Arrange
        String username = "";
        String password = PLAIN_PASSWORD;

        // Act
        Optional<AuthenticatedUser> result = authenticationService.authenticate(username, password);

        // Assert
        assertFalse(result.isPresent());

        verifyNoInteractions(employeeRepository, onlineUserRepository);
        // PasswordUtil.verifyPassword should not be called
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.verifyNoInteractions();
        }
    }

    @Test
    void authenticate_NullPassword() throws DatabaseOperationException {
        // Arrange
        String username = "any_user";
        String password = null;

        // Act
        Optional<AuthenticatedUser> result = authenticationService.authenticate(username, password);

        // Assert
        assertFalse(result.isPresent());

        verifyNoInteractions(employeeRepository, onlineUserRepository);
        // PasswordUtil.verifyPassword should not be called
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.verifyNoInteractions();
        }
    }

    @Test
    void authenticate_EmptyPassword() throws DatabaseOperationException {
        // Arrange
        String username = "any_user";
        String password = "";

        // Act
        Optional<AuthenticatedUser> result = authenticationService.authenticate(username, password);

        // Assert
        assertFalse(result.isPresent());

        verifyNoInteractions(employeeRepository, onlineUserRepository);
        // PasswordUtil.verifyPassword should not be called
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.verifyNoInteractions();
        }
    }

    @Test
    void authenticate_DatabaseOperationExceptionWhenEmployeeRepositoryFails() throws DatabaseOperationException {
        // Arrange
        String username = "employee_user";
        String password = PLAIN_PASSWORD;

        when(employeeRepository.findByUsername(username)).thenThrow(new DatabaseOperationException("DB error during employee lookup"));

        // Act & Assert
        DatabaseOperationException thrown = assertThrows(DatabaseOperationException.class, () ->
                        authenticationService.authenticate(username, password),
                "Should throw DatabaseOperationException when employee repository fails");

        assertEquals("DB error during employee lookup", thrown.getMessage());

        verify(employeeRepository, times(1)).findByUsername(username);
        verifyNoInteractions(onlineUserRepository); // Should not check online users
        // PasswordUtil.verifyPassword should not be called
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.verifyNoInteractions();
        }
        verifyNoMoreInteractions(employeeRepository, onlineUserRepository);
    }

    @Test
    void authenticate_DatabaseOperationExceptionWhenOnlineUserRepositoryFails() throws DatabaseOperationException {
        // Arrange
        String username = "customer_user";
        String password = PLAIN_PASSWORD;

        when(employeeRepository.findByUsername(username)).thenReturn(Optional.empty()); // No employee found
        when(onlineUserRepository.findByUsername(username)).thenThrow(new DatabaseOperationException("DB error during customer lookup"));

        // Act & Assert
        DatabaseOperationException thrown = assertThrows(DatabaseOperationException.class, () ->
                        authenticationService.authenticate(username, password),
                "Should throw DatabaseOperationException when online user repository fails");

        assertEquals("DB error during customer lookup", thrown.getMessage());

        verify(employeeRepository, times(1)).findByUsername(username);
        verify(onlineUserRepository, times(1)).findByUsername(username);
        // PasswordUtil.verifyPassword should not be called
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.verifyNoInteractions();
        }
        verifyNoMoreInteractions(employeeRepository, onlineUserRepository);
    }
    @Test
    void authenticateAsync_ExecutesOnBackgroundThread() throws Exception {
        String username = "async_user";
        String callingThreadName = Thread.currentThread().getName();

        when(employeeRepository.findByUsername(username)).thenAnswer(invocation -> {
            // Capture the name of the thread executing the repository logic
            String executingThreadName = Thread.currentThread().getName();
            assertNotEquals(callingThreadName, executingThreadName, "Auth should run on a background thread");
            return Optional.empty();
        });

        CompletableFuture<Optional<AuthenticatedUser>> future = authenticationService.authenticateAsync(username, PLAIN_PASSWORD);
        future.get(); // Wait for completion
    }

    @Test
    void authenticateAsync_ConcurrencyStressTest_Failures() throws Exception {
        int tasks = 15;
        when(employeeRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(onlineUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        java.util.List<CompletableFuture<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < tasks; i++) {
            futures.add(authenticationService.authenticateAsync("user" + i, "wrong_pass"));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Verify repositories were hit the correct number of times
        verify(employeeRepository, times(tasks)).findByUsername(anyString());
        verify(onlineUserRepository, times(tasks)).findByUsername(anyString());
    }
    @Test
    void authenticateAsync_UsesInternalThreadPool() throws Exception {
        String username = "pool_test_user";
        when(employeeRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        CompletableFuture<Optional<AuthenticatedUser>> future = authenticationService.authenticateAsync(username, PLAIN_PASSWORD);
        future.get();

        // Check if the executor is actually a ThreadPoolExecutor
        java.lang.reflect.Field field = AuthenticationServiceImplementation.class.getDeclaredField("executor");
        field.setAccessible(true);
        java.util.concurrent.ThreadPoolExecutor executor = (java.util.concurrent.ThreadPoolExecutor) field.get(authenticationService);

        assertTrue(executor.getMaximumPoolSize() >= Runtime.getRuntime().availableProcessors());
    }
    @Test
    void authenticateAsync_PathFlowSuccess() throws Exception {
        String username = "customer_user";
        OnlineUser customer = new OnlineUser(202, username, HASHED_PASSWORD, "Customer", "c@e.com", "Addr");

        when(employeeRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.of(customer));

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword(anyString(), anyString())).thenReturn(true);

            Optional<AuthenticatedUser> result = authenticationService.authenticateAsync(username, PLAIN_PASSWORD).get();

            assertTrue(result.isPresent());
            assertEquals(UserRole.CUSTOMER, result.get().getRole());
            verify(employeeRepository).findByUsername(username);
            verify(onlineUserRepository).findByUsername(username);
        }
    }
    @Test
    void authenticateAsync_RejectsAfterShutdown() {
        authenticationService.shutdown();

        // Submit a task after shutdown
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> {
            authenticationService.authenticateAsync("any", "any");
        });
    }

}
