package com.syos.adapter.in.CLI;

import com.syos.domain.exception.DatabaseOperationException;
import com.syos.domain.exception.UserAlreadyExistsException;

import com.syos.application.port.OnlineUserService;

import java.util.Scanner;

public class UserUI {
    private final OnlineUserService onlineUserService;
    private final Scanner scanner;

    public UserUI(OnlineUserService onlineUserService, Scanner scanner) {
        this.onlineUserService = onlineUserService;
        this.scanner = scanner;
    }

    public void displayUserMenu() {
        System.out.println("\n--- ONLINE USER MANAGEMENT ---"); // Changed from full line
        System.out.println("----------------------------------------"); // Single line after title
        System.out.println("1. Register New Online User");
        System.out.println("----------------------------------------"); // Line before exit option
        System.out.println("0. Back to Main Menu");
        System.out.println("----------------------------------------"); // Final line
    }

    public boolean handleUserAction(int choice) {
        switch (choice) {
            case 1:
                handleRegisterNewUser();
                break;
            case 0:
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
        return true;
    }

    private void handleRegisterNewUser() {
        try {
            System.out.println("\n--- REGISTER NEW USER ---"); // Changed from full line
            System.out.println("-----------------------------------"); // Single line after title

            System.out.print("Enter Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Enter Password: ");
            String password = scanner.nextLine().trim();

            System.out.print("Enter Full Name (optional): ");
            String fullName = scanner.nextLine().trim();

            System.out.print("Enter Email: ");
            String email = scanner.nextLine().trim();

            System.out.print("Enter Address (optional): ");
            String address = scanner.nextLine().trim();

            onlineUserService.registerUser(username, password, fullName, email, address);
            System.out.println("User '" + username + "' registered successfully!");

        } catch (UserAlreadyExistsException e) {
            System.out.println("Registration failed: " + e.getMessage());
        } catch (DatabaseOperationException | IllegalArgumentException e) {
            System.out.println("Error registering user: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred during registration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}