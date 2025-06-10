package com.syos.domain.model;


import com.syos.domain.enums.UserRole;

public class AuthenticatedUser {
    private final int id;
    private final String username;
    private final UserRole role;

    public AuthenticatedUser(int id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
}