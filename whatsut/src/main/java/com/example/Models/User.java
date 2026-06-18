package com.example.Models;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String hashPassword;

    public User(String name, String hashPassword) {
        this.name = name;
        this.hashPassword = hashPassword;
    }

    public String GetName() { return name; }
    public boolean VerifyHashPassword(String password) { return hashPassword.equals(password); }
}
