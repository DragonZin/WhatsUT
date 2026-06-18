package com.example.Models;

import java.time.Instant;

public abstract class Message {
    private String content;
    private User sender;
    private Instant timestamp;
}
