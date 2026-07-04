package com.syos.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class OnlineUser {
    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private LocalDateTime registrationDate;
    private String address;

    /**
     * Constructor for creating a new OnlineUser (e.g., during registration).
     */
    public OnlineUser(String username, String passwordHash, String fullName, String email, String address) {
        if (username == null || username.trim().isEmpty()) throw new IllegalArgumentException("Username cannot be null or empty.");
        if (passwordHash == null || passwordHash.trim().isEmpty()) throw new IllegalArgumentException("Password hash cannot be null or empty.");
        if (email == null || email.trim().isEmpty() || !email.contains("@")) throw new IllegalArgumentException("Valid email is required.");

        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.address = address;
        this.registrationDate = LocalDateTime.now();
        this.userId = 0; // Initialize for new users, will be set by repository
    }

    /**
     * Constructor for reconstructing an existing OnlineUser from the database.
     * If registrationDate is null, it defaults to the current time.
     */
    public OnlineUser(int userId, String username, String passwordHash, String fullName, String email, LocalDateTime registrationDate, String address) {
        // Reuse validation from the simpler constructor
        this(username, passwordHash, fullName, email, address);
        this.userId = userId; // Override the 0 set in the chained constructor
        this.registrationDate = (registrationDate != null) ? registrationDate : LocalDateTime.now(); // Override or set if null
    }

    // Additional overloaded constructor for convenience (defaults registrationDate to now)
    // Note: This constructor also chains, so ensure it calls the correct base constructor.
    // Given the existence of the LocalDateTime constructor, this might be redundant if not explicitly used elsewhere.
    // For consistency with typical patterns, if `registrationDate` is optional,
    // the constructor without it should explicitly set `LocalDateTime.now()` or call the full constructor.
    public OnlineUser(int userId, String username, String passwordHash, String fullName, String email, String address) {
        this(userId, username, passwordHash, fullName, email, LocalDateTime.now(), address);
    }

    // Getters and setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    // FIX: Added setUsername
    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) throw new IllegalArgumentException("Username cannot be null or empty.");
        this.username = username;
    }

    public String getPasswordHash() { return passwordHash; }
    // FIX: Added setPasswordHash
    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) throw new IllegalArgumentException("Password hash cannot be null or empty.");
        this.passwordHash = passwordHash;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required.");
        }
        this.email = email;
    }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    // Adding a setter for registrationDate, though it's typically set once
    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }


    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnlineUser that = (OnlineUser) o;
        // Primary key should define equality. If userId is 0 (new unsaved object), then compare by unique business keys.
        if (userId != 0 && that.userId != 0) return userId == that.userId;
        return Objects.equals(username, that.username) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        // If userId is present, use it for hashCode. Otherwise, use unique business keys.
        if (userId != 0) return Objects.hash(userId);
        return Objects.hash(username, email);
    }

    @Override
    public String toString() {
        return "OnlineUser{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", registrationDate=" + registrationDate +
                ", address='" + address + '\'' +
                '}';
    }
}
