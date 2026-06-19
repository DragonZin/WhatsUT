package com.example.Service;

import com.example.Models.Message;
import com.example.Rmi.ClientRemote;
import com.example.Rmi.ServerRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientService extends UnicastRemoteObject implements ClientRemote, AutoCloseable {
    private final String userName;
    private final ServerRemote serverRemote;
    private Runnable groupsRefreshCallback = () -> { };
    private BiConsumer<String, String> requestRefreshCallback = (groupName, requesterName) -> { };
    private BiConsumer<String, Message> messageRefreshCallback = (groupName, message) -> { };
    private BiConsumer<String, Message> privateMessageRefreshCallback = (senderName, message) -> { };
    private Consumer<RemoteException> errorCallback = exception -> { };
    private boolean registered;

    public ClientService(String userName, ServerRemote serverRemote) throws RemoteException {
        super();
        this.userName = requireText(userName, "Nome de usuario");
        this.serverRemote = Objects.requireNonNull(serverRemote, "serverRemote e obrigatorio.");
    }

    public synchronized boolean isRegistered() {
        return registered;
    }

    public String getUserName() {
        return userName;
    }

    public synchronized boolean register(String password) throws RemoteException {
        if (registered) {
            return true;
        }

        registered = serverRemote.login(userName, requireText(password, "Password"), this);
        return registered;
    }

    public synchronized void unregister() throws RemoteException {
        if (!registered) {
            return;
        }

        serverRemote.logout(userName);
        registered = false;
    }

    public void onRefreshGroups(Runnable callback) {
        groupsRefreshCallback = callback == null ? () -> { } : callback;
    }

    public void onRefreshRequest(BiConsumer<String, String> callback) {
        requestRefreshCallback = callback == null ? (groupName, requesterName) -> { } : callback;
    }

    public void onRefreshMessage(BiConsumer<String, Message> callback) {
        messageRefreshCallback = callback == null ? (groupName, message) -> { } : callback;
    }

    public void onRefreshPrivateMessage(BiConsumer<String, Message> callback) {
        privateMessageRefreshCallback = callback == null ? (senderName, message) -> { } : callback;
    }

    public void onError(Consumer<RemoteException> callback) {
        errorCallback = callback == null ? exception -> { } : callback;
    }

    @Override
    public void refreshGroups() throws RemoteException {
        executeCallback(groupsRefreshCallback);
    }

    @Override
    public void refreshRequest(String groupName, String userName) throws RemoteException {
        executeCallback(() -> requestRefreshCallback.accept(groupName, userName));
    }

    @Override
    public void refreshMessage(String groupName, Message message) throws RemoteException {
        executeCallback(() -> messageRefreshCallback.accept(groupName, message));
    }
    
    @Override
    public void refreshPrivateMessage(String senderName, Message message) throws RemoteException {
        executeCallback(() -> privateMessageRefreshCallback.accept(senderName, message));
    }

    @Override
    public void close() throws RemoteException {
        try {
            unregister();
        } finally {
            UnicastRemoteObject.unexportObject(this, true);
        }
    }

    private void executeCallback(Runnable callback) throws RemoteException {
        try {
            callback.run();
        } catch (RuntimeException exception) {
            RemoteException remoteException = new RemoteException("Erro ao executar callback do cliente.", exception);
            errorCallback.accept(remoteException);
            throw remoteException;
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return value;
    }
}