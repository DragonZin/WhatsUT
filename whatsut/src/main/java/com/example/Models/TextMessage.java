package com.example.Models;

public class TextMessage extends Message {
    private String content;

    public TextMessage(String content, User sender) {
        super(sender);
        this.content = content;
    }

    public String getContent() { return content; }
}
