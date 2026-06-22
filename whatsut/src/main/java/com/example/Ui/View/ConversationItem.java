package com.example.Ui.View;

import com.example.Models.Group;
import com.example.Models.User;

public final class ConversationItem {
    public enum Type { PRIVATE, GROUP }

    private final Type type;
    private final User user;
    private final Group group;
    private final boolean online;
    private final int unreadCount;

    private ConversationItem(Type type, User user, Group group, boolean online, int unreadCount) {
        this.type = type;
        this.user = user;
        this.group = group;
        this.online = online;
        this.unreadCount = unreadCount;
    }

    public static ConversationItem privateUser(User user, boolean online) {
        return privateUser(user, online, 0);
    }

    public static ConversationItem privateUser(User user, boolean online, int unreadCount) {
        return new ConversationItem(Type.PRIVATE, user, null, online, unreadCount);
    }

    public static ConversationItem group(Group group) {
        return group(group, 0);
    }

    public static ConversationItem group(Group group, int unreadCount) {
        return new ConversationItem(Type.GROUP, null, group, false, unreadCount);
    }

    public Type type() { return type; }
    public User user() { return user; }
    public Group group() { return group; }
    public boolean online() { return online; }
    public int unreadCount() { return unreadCount; }
    public boolean unread() { return unreadCount > 0; }
    public String key() { return (type == Type.PRIVATE ? "P:" : "G:") + name(); }
    public String name() { return type == Type.PRIVATE ? user.GetName() : group.getName(); }
}