package com.example.Ui.View;

import com.example.Models.Group;
import com.example.Models.User;

public final class ConversationItem {
    public enum Type { PRIVATE, GROUP }

    private final Type type;
    private final User user;
    private final Group group;
    private final boolean online;

    private ConversationItem(Type type, User user, Group group, boolean online) {
        this.type = type;
        this.user = user;
        this.group = group;
        this.online = online;
    }

    public static ConversationItem privateUser(User user, boolean online) {
        return new ConversationItem(Type.PRIVATE, user, null, online);
    }

    public static ConversationItem group(Group group) {
        return new ConversationItem(Type.GROUP, null, group, false);
    }

    public Type type() { return type; }
    public User user() { return user; }
    public Group group() { return group; }
    public boolean online() { return online; }
    public String name() { return type == Type.PRIVATE ? user.GetName() : group.getName(); }
}