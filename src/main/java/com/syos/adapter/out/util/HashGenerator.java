package com.syos.adapter.out.util; // You can place this in any package, or just create a temporary one

public class HashGenerator {
    public static void main(String[] args) {
        String adminPassword = "12345678";
        String staffPassword = "staffpass"; // Assuming you want a specific password for staff_user

        String hashedAdminPassword = PasswordUtil.hashPassword(adminPassword);
        String hashedStaffPassword = PasswordUtil.hashPassword(staffPassword);

        System.out.println("Plain Password: " + adminPassword + " -> Hashed: " + hashedAdminPassword);
        System.out.println("Plain Password: " + staffPassword + " -> Hashed: " + hashedStaffPassword);
    }
}
    