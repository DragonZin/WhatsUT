package com.example.Models;

public class User {
    private String name;
    private String hashPassword;

    public User(String name, String hashPassword) {
        this.name = name;
        this.hashPassword = hashPassword;
    }
}
