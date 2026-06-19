package com.example.Utils;

public record ConversationKey(String user1, String user2) {

    public ConversationKey {
        if (user1.compareTo(user2) > 0) {
            String temp = user1;
            user1 = user2;
            user2 = temp;
        }
    }
}