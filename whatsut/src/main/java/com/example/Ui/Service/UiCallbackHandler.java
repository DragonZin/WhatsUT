package com.example.Ui.Service;

import java.rmi.RemoteException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.example.Models.Message;

import javafx.application.Platform;

public class UiCallbackHandler {
    private Runnable groupsRefreshHandler = () -> { };
    private BiConsumer<String, String> joinRequestHandler = (groupName, requesterName) -> { };
    private BiConsumer<String, Message> groupMessageHandler = (groupName, message) -> { };
    private BiConsumer<String, Message> privateMessageHandler = (senderName, message) -> { };
    private Consumer<RemoteException> errorHandler = exception -> { };

    public void onGroupsRefresh(Runnable handler) {
        groupsRefreshHandler = handler == null ? () -> { } : handler;
    }

    public void onJoinRequest(BiConsumer<String, String> handler) {
        joinRequestHandler = handler == null ? (groupName, requesterName) -> { } : handler;
    }

    public void onGroupMessage(BiConsumer<String, Message> handler) {
        groupMessageHandler = handler == null ? (groupName, message) -> { } : handler;
    }

    public void onPrivateMessage(BiConsumer<String, Message> handler) {
        privateMessageHandler = handler == null ? (senderName, message) -> { } : handler;
    }

    public void onError(Consumer<RemoteException> handler) {
        errorHandler = handler == null ? exception -> { } : handler;
    }

    void handleGroupsRefresh() {
        Platform.runLater(groupsRefreshHandler);
    }

    void handleJoinRequest(String groupName, String requesterName) {
        Platform.runLater(() -> joinRequestHandler.accept(groupName, requesterName));
    }

    void handleGroupMessage(String groupName, Message message) {
        Platform.runLater(() -> groupMessageHandler.accept(groupName, message));
    }

    void handlePrivateMessage(String senderName, Message message) {
        Platform.runLater(() -> privateMessageHandler.accept(senderName, message));
    }

    void handleError(RemoteException exception) {
        Platform.runLater(() -> errorHandler.accept(exception));
    }
}