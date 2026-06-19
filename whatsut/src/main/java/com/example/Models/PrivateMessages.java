package com.example.Models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PrivateMessages implements Serializable {
    private static final long serialVersionUID = 1L;
    private String user1;
    private String user2;
    private List<Message> messages;

    public PrivateMessages(String user1, String user2) {
        this.user1 = user1;
        this.user2 = user2;
        this.messages = new ArrayList<>();
    }
    
    public String getUser1() { return user1; }
    public String getUser2() { return user2; }

    public List<Message> getMessages() { return messages; }
    public boolean addMessage(Message message) { return messages.add(message); }
}