package com.example.Models;

import java.util.List;

public class Group {
    private String name;
    private List<User> members;
    private List<User> pendingMembers;
    private User admin;
    private List<Message> messages;

    public Group(String name, User admin) {
        this.name = name;
        this.members.add(admin);
        this.admin = admin;    
    }

    public String getName() { return name; }
    
    public List<User> getMembers() { return members; }
    public boolean hasMember(User user) { return members.contains(user); }
        
    public List<User> getPendingMembers() { return pendingMembers; }
    public boolean hasPendingMember(User user) { return pendingMembers.contains(user); }
    public void addPendingMember(User user) {
        if (hasPendingMember(user)) return;
        pendingMembers.add(user);
    }
    public boolean approvePendingMember(User user) {
        if (!hasPendingMember(user)) return false;
        pendingMembers.remove(user);
        members.add(user);
        return true;
    }

    public User getAdmin() { return admin; }

    public List<Message> getMessages() { return messages; }
    public void addMessage(Message message) {
        messages.add(message);
    }
}
