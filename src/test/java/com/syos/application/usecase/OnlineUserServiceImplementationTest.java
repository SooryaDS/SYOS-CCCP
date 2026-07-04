package com.syos.application.usecase;

import com.syos.adapter.out.DatabaseSpecificAdaptors.repository.OnlineUserRepository;
import com.syos.adapter.out.util.PasswordUtil;
import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;
import com.syos.domain.exception.UserNotFoundException;
import com.syos.domain.model.OnlineUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OnlineUserServiceImplementationTest {

    @Mock
    private OnlineUserRepository onlineUserRepository;

    @InjectMocks
    private OnlineUserServiceImplementation onlineUserService;

    private OnlineUser sampleUser;
    private OnlineUser anotherSampleUser; // Needed for update conflict tests
    private String plainPassword;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        plainPassword = "SecurePass123";
        // Directly use PasswordUtil for hashing to ensure consistency with the service logic
        hashedPassword = PasswordUtil.hashPassword(plainPassword);
        // Corrected OnlineUser constructor call to include LocalDateTime for registrationDate and address
        sampleUser = new OnlineUser(1, "testuser", hashedPassword, "Test User", "test@example.com", LocalDateTime.now(), "123 Main St");
        // Initialize anotherSampleUser for update conflict tests
        anotherSampleUser = new OnlineUser(2, "anotheruser", PasswordUtil.hashPassword("pass456"), "Another User", "another@example.com", LocalDateTime.now(), "456 Side Ave");
    }

    // --- Tests for registerUser (Existing, but including for context of total count) ---

    @Test
    void registerUser_SuccessfulRegistration() throws UserAlreadyExistsException, DatabaseOperationException {
        // Given
        String username = "newuser";
        String email = "newuser@example.com";
        String address = "456 Oak Ave";

        // Mock repository behavior: username and email are not found
        when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(onlineUserRepository.findByEmail(email)).thenReturn(Optional.empty());
        // Mock save operation to return a user with a generated ID
        when(onlineUserRepository.save(any(OnlineUser.class))).thenAnswer(invocation -> {
            OnlineUser user = invocation.getArgument(0);
            user.setUserId(101); // Simulate database assigning an ID
            return user;
        });

        // When
        OnlineUser registeredUser = onlineUserService.registerUser(username, plainPassword, "New Registered User", email, address);

        // Then
        assertNotNull(registeredUser);
        assertEquals(101, registeredUser.getUserId());
        assertEquals(username, registeredUser.getUsername());
        assertTrue(PasswordUtil.verifyPassword(plainPassword, registeredUser.getPasswordHash()), "Password should be correctly hashed and verifiable");
        assertEquals(email, registeredUser.getEmail());
        assertEquals(address, registeredUser.getAddress());

        verify(onlineUserRepository, times(1)).findByUsername(username);
        verify(onlineUserRepository, times(1)).findByEmail(email);
        verify(onlineUserRepository, times(1)).save(any(OnlineUser.class));
    }

    @Test
    void registerUser_ThrowsUserAlreadyExistsException_ForExistingUsername() throws DatabaseOperationException {
        // Given
        String existingUsername = "testuser";
        String newEmail = "unique@example.com"; // Email is unique, but username is not

        // Mock repository behavior: username found
        when(onlineUserRepository.findByUsername(existingUsername)).thenReturn(Optional.of(sampleUser));

        // When & Then
        UserAlreadyExistsException thrown = assertThrows(UserAlreadyExistsException.class, () ->
                onlineUserService.registerUser(existingUsername, plainPassword, "Some User", newEmail, "Some Address")
        );
        assertEquals("Username '" + existingUsername + "' is already taken.", thrown.getMessage());

        verify(onlineUserRepository, times(1)).findByUsername(existingUsername);
        verify(onlineUserRepository, never()).findByEmail(anyString()); // Email check should not be reached
        verify(onlineUserRepository, never()).save(any(OnlineUser.class));
    }

    @Test
    void registerUser_ThrowsUserAlreadyExistsException_ForExistingEmail() throws DatabaseOperationException {
        // Given
        String newUsername = "anotheruser"; // Username is unique, but email is not
        String existingEmail = "test@example.com";

        // Mock repository behavior: username not found, but email found
        when(onlineUserRepository.findByUsername(newUsername)).thenReturn(Optional.empty());
        when(onlineUserRepository.findByEmail(existingEmail)).thenReturn(Optional.of(sampleUser));

        // When & Then
        UserAlreadyExistsException thrown = assertThrows(UserAlreadyExistsException.class, () ->
                onlineUserService.registerUser(newUsername, plainPassword, "Another User", existingEmail, "Another Address")
        );
        assertEquals("Email '" + existingEmail + "' is already registered.", thrown.getMessage());

        verify(onlineUserRepository, times(1)).findByUsername(newUsername);
        verify(onlineUserRepository, times(1)).findByEmail(existingEmail);
        verify(onlineUserRepository, never()).save(any(OnlineUser.class));
    }

    @Test
    void registerUser_ThrowsIllegalArgumentException_ForEmptyUsername() {
        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.registerUser(" ", plainPassword, "Full Name", "email@test.com", "Address")
        );
        assertEquals("Username cannot be empty.", thrown.getMessage());
        verifyNoInteractions(onlineUserRepository);
    }

    @Test
    void registerUser_ThrowsIllegalArgumentException_ForNullPassword() {
        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.registerUser("user", null, "Full Name", "email@test.com", "Address")
        );
        assertEquals("Password cannot be empty.", thrown.getMessage());
        verifyNoInteractions(onlineUserRepository);
    }

    @Test
    void registerUser_ThrowsIllegalArgumentException_ForInvalidEmailFormat() {
        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.registerUser("user", plainPassword, "Full Name", "invalid-email", "Address")
        );
        assertEquals("A valid email is required.", thrown.getMessage());
        verifyNoInteractions(onlineUserRepository);
    }

    @Test
    void registerUser_ThrowsDatabaseOperationException_OnSaveFailure() throws DatabaseOperationException {
        // Given
        String username = "newuser_dbfail";
        String email = "newuser_dbfail@example.com";

        when(onlineUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(onlineUserRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(onlineUserRepository.save(any(OnlineUser.class))).thenThrow(new DatabaseOperationException("DB connection lost"));

        // When & Then
        DatabaseOperationException thrown = assertThrows(DatabaseOperationException.class, () ->
                onlineUserService.registerUser(username, plainPassword, "DB Error User", email, "Address")
        );
        assertEquals("DB connection lost", thrown.getMessage());

        verify(onlineUserRepository, times(1)).findByUsername(username);
        verify(onlineUserRepository, times(1)).findByEmail(email);
        verify(onlineUserRepository, times(1)).save(any(OnlineUser.class));
    }

    // --- Tests for authenticateUser ---

    @Test
    void authenticateUser_SuccessfulAuthentication() throws DatabaseOperationException {
        // Given: The user exists and the password is correct
        OnlineUser foundUser = new OnlineUser(1, "testuser", hashedPassword, "Test User", "test@example.com", LocalDateTime.now(), "123 Main St");
        when(onlineUserRepository.findByUsername(sampleUser.getUsername())).thenReturn(Optional.of(foundUser));

        // When
        Optional<OnlineUser> authenticatedUser = onlineUserService.authenticateUser(sampleUser.getUsername(), plainPassword);

        // Then
        assertTrue(authenticatedUser.isPresent(), "User should be authenticated");
        assertEquals(sampleUser.getUsername(), authenticatedUser.get().getUsername());
        verify(onlineUserRepository, times(1)).findByUsername(sampleUser.getUsername());
    }

    @Test
    void authenticateUser_FailedAuthentication_WrongPassword() throws DatabaseOperationException {
        // Given: User exists but password is wrong
        OnlineUser foundUser = new OnlineUser(1, "testuser", hashedPassword, "Test User", "test@example.com", LocalDateTime.now(), "123 Main St");
        when(onlineUserRepository.findByUsername(sampleUser.getUsername())).thenReturn(Optional.of(foundUser));

        // When
        Optional<OnlineUser> authenticatedUser = onlineUserService.authenticateUser(sampleUser.getUsername(), "wrongpassword");

        // Then
        assertFalse(authenticatedUser.isPresent(), "Authentication should fail for wrong password");
        verify(onlineUserRepository, times(1)).findByUsername(sampleUser.getUsername());
    }

    @Test
    void authenticateUser_FailedAuthentication_UserNotFound() throws DatabaseOperationException {
        // Given: User does not exist
        String nonexistentUsername = "nonexistent";
        when(onlineUserRepository.findByUsername(nonexistentUsername)).thenReturn(Optional.empty());

        // When
        Optional<OnlineUser> authenticatedUser = onlineUserService.authenticateUser(nonexistentUsername, plainPassword);

        // Then
        assertFalse(authenticatedUser.isPresent(), "Authentication should fail if user not found");
        verify(onlineUserRepository, times(1)).findByUsername(nonexistentUsername);
    }

    @Test
    void authenticateUser_ReturnsEmptyOptional_ForNullUsername() throws DatabaseOperationException {
        // When
        Optional<OnlineUser> authenticatedUser = onlineUserService.authenticateUser(null, plainPassword);

        // Then
        assertFalse(authenticatedUser.isPresent(), "Should return empty for null username");
        verifyNoInteractions(onlineUserRepository); // Repository should not be called
    }

    @Test
    void authenticateUser_ReturnsEmptyOptional_ForEmptyPassword() throws DatabaseOperationException {
        // When
        Optional<OnlineUser> authenticatedUser = onlineUserService.authenticateUser(sampleUser.getUsername(), "");

        // Then
        assertFalse(authenticatedUser.isPresent(), "Should return empty for empty password");
        verifyNoInteractions(onlineUserRepository); // Repository should not be called
    }

    @Test
    void authenticateUser_ThrowsDatabaseOperationException_OnRepositoryFailure() throws DatabaseOperationException {
        // Given: Repository throws an exception during findByUsername
        when(onlineUserRepository.findByUsername(sampleUser.getUsername())).thenThrow(new DatabaseOperationException("DB error during find"));

        // When & Then
        DatabaseOperationException thrown = assertThrows(DatabaseOperationException.class, () ->
                onlineUserService.authenticateUser(sampleUser.getUsername(), plainPassword)
        );
        assertEquals("DB error during find", thrown.getMessage());

        verify(onlineUserRepository, times(1)).findByUsername(sampleUser.getUsername());
    }

    // --- NEW TESTS (10+ tests for update, delete, getById, getByUsername, getAll) ---

    // --- Tests for updateUser ---
    @Test
    void updateUser_SuccessfulUpdate() throws DatabaseOperationException, UserNotFoundException, IllegalArgumentException {
        OnlineUser userToUpdate = new OnlineUser(sampleUser.getUserId(), sampleUser.getUsername(), sampleUser.getPasswordHash(),
                "Updated Name", "updated@example.com", sampleUser.getRegistrationDate(), "New Address");

        // Mock repository behavior: user found, no username/email conflict
        when(onlineUserRepository.findById(userToUpdate.getUserId())).thenReturn(Optional.of(sampleUser));
        when(onlineUserRepository.findByUsername(userToUpdate.getUsername())).thenReturn(Optional.of(sampleUser)); // Same user, no conflict
        when(onlineUserRepository.findByEmail(userToUpdate.getEmail())).thenReturn(Optional.empty()); // New email, no conflict
        when(onlineUserRepository.update(any(OnlineUser.class))).thenReturn(userToUpdate);

        OnlineUser result = onlineUserService.updateUser(userToUpdate);

        assertNotNull(result);
        assertEquals("Updated Name", result.getFullName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("New Address", result.getAddress());
        verify(onlineUserRepository, times(1)).findById(userToUpdate.getUserId());
        verify(onlineUserRepository, times(1)).findByUsername(userToUpdate.getUsername());
        verify(onlineUserRepository, times(1)).findByEmail(userToUpdate.getEmail());
        verify(onlineUserRepository, times(1)).update(userToUpdate);
    }

    @Test
    void updateUser_ThrowsUserNotFoundException() throws DatabaseOperationException {
        OnlineUser userToUpdate = new OnlineUser(999, "nonexistent", hashedPassword, "Non Existent", "no@example.com", LocalDateTime.now(), "N/A");
        when(onlineUserRepository.findById(userToUpdate.getUserId())).thenReturn(Optional.empty());

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () ->
                onlineUserService.updateUser(userToUpdate)
        );
        assertEquals("User with ID " + userToUpdate.getUserId() + " not found for update.", thrown.getMessage());
        verify(onlineUserRepository, times(1)).findById(userToUpdate.getUserId());
        verify(onlineUserRepository, never()).update(any(OnlineUser.class));
    }

    @Test
    void updateUser_ThrowsIllegalArgumentException_ForInvalidUserId() {
        OnlineUser userToUpdate = new OnlineUser(0, "user", hashedPassword, "Name", "email@test.com", LocalDateTime.now(), "Addr"); // ID 0
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.updateUser(userToUpdate)
        );
        assertEquals("User ID must be positive for update.", thrown.getMessage());
        verifyNoInteractions(onlineUserRepository);
    }

    @Test
    void updateUser_ThrowsIllegalArgumentException_ForConflictingUsername() throws DatabaseOperationException {
        OnlineUser userToUpdate = new OnlineUser(sampleUser.getUserId(), anotherSampleUser.getUsername(), hashedPassword, "Updated Name", "updated@example.com", sampleUser.getRegistrationDate(), "New Address");

        // Mock that the original user exists
        when(onlineUserRepository.findById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        // Mock that the new username is already taken by a different user (anotherSampleUser)
        when(onlineUserRepository.findByUsername(anotherSampleUser.getUsername())).thenReturn(Optional.of(anotherSampleUser));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.updateUser(userToUpdate)
        );
        assertEquals("Username '" + anotherSampleUser.getUsername() + "' is already taken by another user.", thrown.getMessage());
        verify(onlineUserRepository, times(1)).findById(sampleUser.getUserId());
        verify(onlineUserRepository, times(1)).findByUsername(anotherSampleUser.getUsername());
        verify(onlineUserRepository, never()).update(any(OnlineUser.class));
    }

    @Test
    void updateUser_ThrowsIllegalArgumentException_ForConflictingEmail() throws DatabaseOperationException {
        OnlineUser userToUpdate = new OnlineUser(sampleUser.getUserId(), sampleUser.getUsername(), hashedPassword, "Updated Name", anotherSampleUser.getEmail(), sampleUser.getRegistrationDate(), "New Address");

        // Mock that the original user exists
        when(onlineUserRepository.findById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        // Mock that the new email is already taken by a different user (anotherSampleUser)
        when(onlineUserRepository.findByUsername(sampleUser.getUsername())).thenReturn(Optional.of(sampleUser)); // Username check passes for current user
        when(onlineUserRepository.findByEmail(anotherSampleUser.getEmail())).thenReturn(Optional.of(anotherSampleUser));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.updateUser(userToUpdate)
        );
        assertEquals("Email '" + anotherSampleUser.getEmail() + "' is already registered by another user.", thrown.getMessage());
        verify(onlineUserRepository, times(1)).findById(sampleUser.getUserId());
        verify(onlineUserRepository, times(1)).findByUsername(sampleUser.getUsername());
        verify(onlineUserRepository, times(1)).findByEmail(anotherSampleUser.getEmail());
        verify(onlineUserRepository, never()).update(any(OnlineUser.class));
    }


    @Test
    void updateUser_ThrowsDatabaseOperationException_OnRepositoryFailure() throws DatabaseOperationException, UserNotFoundException {
        OnlineUser userToUpdate = new OnlineUser(sampleUser.getUserId(), sampleUser.getUsername(), hashedPassword, "Updated Name", "updated@example.com", sampleUser.getRegistrationDate(), "New Address");
        when(onlineUserRepository.findById(userToUpdate.getUserId())).thenReturn(Optional.of(sampleUser));
        when(onlineUserRepository.findByUsername(userToUpdate.getUsername())).thenReturn(Optional.of(sampleUser));
        when(onlineUserRepository.findByEmail(userToUpdate.getEmail())).thenReturn(Optional.empty());
        when(onlineUserRepository.update(any(OnlineUser.class))).thenThrow(new DatabaseOperationException("DB update failed"));

        DatabaseOperationException thrown = assertThrows(DatabaseOperationException.class, () ->
                onlineUserService.updateUser(userToUpdate)
        );
        assertEquals("DB update failed", thrown.getMessage());
        verify(onlineUserRepository, times(1)).findById(userToUpdate.getUserId());
        verify(onlineUserRepository, times(1)).findByUsername(userToUpdate.getUsername());
        verify(onlineUserRepository, times(1)).findByEmail(userToUpdate.getEmail());
        verify(onlineUserRepository, times(1)).update(userToUpdate);
    }

    // --- Tests for deleteUser ---

    @Test
    void deleteUser_Successful() throws DatabaseOperationException, UserNotFoundException {
        int userIdToDelete = sampleUser.getUserId();
        when(onlineUserRepository.findById(userIdToDelete)).thenReturn(Optional.of(sampleUser)); // User exists
        doNothing().when(onlineUserRepository).delete(userIdToDelete);

        onlineUserService.deleteUser(userIdToDelete);

        verify(onlineUserRepository, times(1)).findById(userIdToDelete);
        verify(onlineUserRepository, times(1)).delete(userIdToDelete);
    }

    @Test
    void deleteUser_ThrowsUserNotFoundException_IfUserDoesNotExist() throws DatabaseOperationException {
        int userIdToDelete = 999;
        when(onlineUserRepository.findById(userIdToDelete)).thenReturn(Optional.empty()); // User does not exist

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () ->
                onlineUserService.deleteUser(userIdToDelete)
        );
        assertEquals("User with ID " + userIdToDelete + " not found for deletion.", thrown.getMessage());
        verify(onlineUserRepository, times(1)).findById(userIdToDelete);
        verify(onlineUserRepository, never()).delete(anyInt());
    }

    @Test
    void deleteUser_ThrowsIllegalArgumentException_ForInvalidUserId() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.deleteUser(0) // Invalid ID
        );
        assertEquals("User ID must be positive for deletion.", thrown.getMessage());
        verifyNoInteractions(onlineUserRepository);
    }

    @Test
    void deleteUser_ThrowsDatabaseOperationException_OnRepositoryFailure() throws DatabaseOperationException, UserNotFoundException {
        int userIdToDelete = sampleUser.getUserId();
        when(onlineUserRepository.findById(userIdToDelete)).thenReturn(Optional.of(sampleUser));
        doThrow(new DatabaseOperationException("DB delete failed")).when(onlineUserRepository).delete(userIdToDelete);

        DatabaseOperationException thrown = assertThrows(DatabaseOperationException.class, () ->
                onlineUserService.deleteUser(userIdToDelete)
        );
        assertEquals("DB delete failed", thrown.getMessage());
        verify(onlineUserRepository, times(1)).findById(userIdToDelete);
        verify(onlineUserRepository, times(1)).delete(userIdToDelete);
    }

    // --- Tests for getUserById ---

    @Test
    void getUserById_SuccessfulRetrieval() throws DatabaseOperationException {
        int userIdToGet = sampleUser.getUserId();
        when(onlineUserRepository.findById(userIdToGet)).thenReturn(Optional.of(sampleUser));

        Optional<OnlineUser> result = onlineUserService.getUserById(userIdToGet);

        assertTrue(result.isPresent());
        assertEquals(userIdToGet, result.get().getUserId());
        verify(onlineUserRepository, times(1)).findById(userIdToGet);
    }

    @Test
    void getUserById_ReturnsEmptyOptional_WhenNotFound() throws DatabaseOperationException {
        int userIdToGet = 999;
        when(onlineUserRepository.findById(userIdToGet)).thenReturn(Optional.empty());

        Optional<OnlineUser> result = onlineUserService.getUserById(userIdToGet);

        assertTrue(result.isEmpty());
        verify(onlineUserRepository, times(1)).findById(userIdToGet);
    }

    @Test
    void getUserById_ThrowsIllegalArgumentException_ForInvalidUserId() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.getUserById(0) // Invalid ID
        );
        assertEquals("User ID must be positive for retrieval.", thrown.getMessage());
        verifyNoInteractions(onlineUserRepository);
    }

    // --- Tests for getUserByUsername ---

    @Test
    void getUserByUsername_SuccessfulRetrieval() throws DatabaseOperationException {
        String usernameToGet = sampleUser.getUsername();
        when(onlineUserRepository.findByUsername(usernameToGet)).thenReturn(Optional.of(sampleUser));

        Optional<OnlineUser> result = onlineUserService.getUserByUsername(usernameToGet);

        assertTrue(result.isPresent());
        assertEquals(usernameToGet, result.get().getUsername());
        verify(onlineUserRepository, times(1)).findByUsername(usernameToGet);
    }

    @Test
    void getUserByUsername_ReturnsEmptyOptional_WhenNotFound() throws DatabaseOperationException {
        String usernameToGet = "nonexistent_user";
        when(onlineUserRepository.findByUsername(usernameToGet)).thenReturn(Optional.empty());

        Optional<OnlineUser> result = onlineUserService.getUserByUsername(usernameToGet);

        assertTrue(result.isEmpty());
        verify(onlineUserRepository, times(1)).findByUsername(usernameToGet);
    }

    @Test
    void getUserByUsername_ThrowsIllegalArgumentException_ForNullUsername() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                onlineUserService.getUserByUsername(null)
        );
        assertEquals("Username for retrieval cannot be null or empty.", thrown.getMessage());
        verifyNoInteractions(onlineUserRepository);
    }

    // --- Tests for getAllUsers ---

    @Test
    void getAllUsers_SuccessfulRetrieval() throws DatabaseOperationException {
        List<OnlineUser> mockUsers = Arrays.asList(sampleUser, anotherSampleUser);
        when(onlineUserRepository.findAll()).thenReturn(mockUsers);

        List<OnlineUser> allUsers = onlineUserService.getAllUsers();

        assertNotNull(allUsers);
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.contains(sampleUser));
        assertTrue(allUsers.contains(anotherSampleUser));
        verify(onlineUserRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_ReturnsEmptyListWhenNoUsers() throws DatabaseOperationException {
        when(onlineUserRepository.findAll()).thenReturn(Collections.emptyList());

        List<OnlineUser> allUsers = onlineUserService.getAllUsers();

        assertNotNull(allUsers);
        assertTrue(allUsers.isEmpty());
        verify(onlineUserRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_ThrowsDatabaseOperationException_OnRepositoryFailure() throws DatabaseOperationException {
        when(onlineUserRepository.findAll()).thenThrow(new DatabaseOperationException("DB error during findAll"));

        DatabaseOperationException thrown = assertThrows(DatabaseOperationException.class, () ->
                onlineUserService.getAllUsers()
        );
        assertEquals("DB error during findAll", thrown.getMessage());
        verify(onlineUserRepository, times(1)).findAll();
    }
}
