package com.example.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private List<User> members;
    private List<User> pendingMembers;
    private User admin;
    private List<Message> messages;

    public Group(String name, User admin) {
        this.name = name;
        this.members = new ArrayList<>();
        this.members.add(admin);
        this.pendingMembers = new ArrayList<>();
        this.admin = admin;
        this.messages = new ArrayList<>();    
    }

    public String getName() { return name; }
    
    public List<User> getMembers() { return members; }
    public boolean hasMember(User user) { return members.contains(user); }
    public boolean removeMember(User user) { return members.remove(user); }
        
    public List<User> getPendingMembers() { return pendingMembers; }
    public boolean hasPendingMember(User user) { return pendingMembers.contains(user); }
    public boolean addPendingMember(User user) {
        if (hasPendingMember(user)) return false;
        return pendingMembers.add(user);
    }
    public boolean approvePendingMember(User user) {
        if (!hasPendingMember(user)) return false;
        pendingMembers.remove(user);
        members.add(user);
        return true;
    }

    public User getAdmin() { return admin; }

    public List<Message> getMessages() { return messages; }
    public boolean addMessage(Message message) { return messages.add(message); }
}
