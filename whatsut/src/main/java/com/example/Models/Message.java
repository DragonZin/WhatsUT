package com.example.Models;

import java.io.Serializable;
import java.time.Instant;

public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private User sender;
    private Instant timestamp;

    public Message(User sender) {
        this.sender = sender;
        this.timestamp = Instant.now();
    }

    public User getSender() { return sender; }
    public Instant getTimestamp() { return timestamp; }
}
