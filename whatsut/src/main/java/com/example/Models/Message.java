package com.example.Models;

import java.time.Instant;

public abstract class Message {
    private String content;
    private User sender;
    private Instant timestamp;
    private FileMessage fileMessage;

    public Message(String content, User sender, FileMessage fileMessage) {
        this.content = content;
        this.sender = sender;
        this.timestamp = Instant.now();
        this.fileMessage = fileMessage;
    }

    public String getContent() { return content; }
    public User getSender() { return sender; }
    public Instant getTimestamp() { return timestamp; }
    public FileMessage getFileMessage() { return fileMessage; }
}
