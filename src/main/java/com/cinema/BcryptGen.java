package com.cinema;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder e = new BCryptPasswordEncoder(10);
        String[] accounts = {
            "Admin1234",    "admin",
            "Manager123",   "manager",
            "Staff1234",    "staff",
            "Customer123",   "customer"
        };
        for (int i = 0; i < accounts.length; i += 2) {
            String pw = accounts[i];
            String u  = accounts[i + 1];
            System.out.println(u + ": " + e.encode(pw));
        }
    }
}
