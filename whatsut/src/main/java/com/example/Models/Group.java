package com.example.Models;

import java.util.List;

public class Group {
    private String name;
    private List<User> members;
    private List<User> pendingMembers;
    private User admin;
    private List<Message> messages;

    public Group(String name, List<User> members, List<User> pendingMembers, User admin, List<Message> messages) {
        this.name = name;
        this.members = members;
        this.pendingMembers = pendingMembers;
        this.admin = admin;
        this.messages = messages;    
    }
}
