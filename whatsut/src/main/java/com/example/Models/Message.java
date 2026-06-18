package com.example.Models;

import java.time.Instant;

public class Message {
    private String content;
    private User sender;
    private Instant timestamp;

    public Message(String content, User sender) {
        this.content = content;
        this.sender = sender;
        this.timestamp = Instant.now();
    }
}
