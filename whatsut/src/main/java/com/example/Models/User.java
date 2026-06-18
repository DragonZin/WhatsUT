package com.example.Models;

public class User {
    private String name;
    private String hashPassword;
    private Status status;

    public User(String name, String hashPassword, Status status) {
        this.name = name;
        this.hashPassword = hashPassword;
        this.status = status;
    }
}

enum Status {
    ONLINE,
    OFFLINE
}