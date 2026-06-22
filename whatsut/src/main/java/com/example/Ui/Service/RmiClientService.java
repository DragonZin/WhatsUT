package com.example.Ui.Service;

import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.User;
import com.example.Rmi.ServerRemote;
import com.example.Service.ClientService;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;

public class RmiClientService implements AutoCloseable {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1099;
    private static final String DEFAULT_SERVICE_NAME = "WhatsUT";

    private final ServerRemote serverRemote;
    private final UiCallbackHandler callbackHandler;
    private ClientService clientService;
    private String currentUser;

    public RmiClientService(UiCallbackHandler callbackHandler) throws Exception {
        this(callbackHandler, lookupRemote());
    }

    RmiClientService(UiCallbackHandler callbackHandler, ServerRemote serverRemote) {
        this.callbackHandler = callbackHandler;
        this.serverRemote = serverRemote;
    }

    public void registerUser(String userName, String password) throws RemoteException {
        serverRemote.registerUser(userName, password);
    }

    public void login(String userName, String password) throws RemoteException {
        closeCurrentSession();
        ClientService service = new ClientService(userName, serverRemote);
        service.onRefreshGroups(callbackHandler::handleGroupsRefresh);
        service.onRefreshRequest(callbackHandler::handleJoinRequest);
        service.onRefreshMessage(callbackHandler::handleGroupMessage);
        service.onRefreshPrivateMessage(callbackHandler::handlePrivateMessage);
        service.onError(callbackHandler::handleError);
        service.register(password);
        clientService = service;
        currentUser = userName;
    }

    public List<User> listOnlineUsers() throws RemoteException {
        return serverRemote.listAuthenticatedUsers(requireCurrentUser());
    }

    public List<User> listUsers() throws RemoteException {
        return serverRemote.listUsers(requireCurrentUser());
    }

    public List<Group> listGroups() throws RemoteException {
        return serverRemote.listGroups(requireCurrentUser());
    }

    public Group createGroup(String groupName) throws RemoteException {
        return serverRemote.createGroup(groupName, requireCurrentUser());
    }

    public boolean requestJoinGroup(String groupName) throws RemoteException {
        return serverRemote.requestJoinGroup(groupName, requireCurrentUser());
    }

    public boolean approvePendingMember(String groupName, String userName) throws RemoteException {
        return serverRemote.approvePendingMember(groupName, requireCurrentUser(), userName);
    }

    public List<Message> getGroupMessages(String groupName) throws RemoteException {
        return serverRemote.getMessages(groupName, requireCurrentUser());
    }

    public List<Message> getPrivateMessages(String otherUser) throws RemoteException {
        return serverRemote.getPrivateMessages(requireCurrentUser(), otherUser);
    }

    public void sendGroupMessage(String groupName, String content) throws RemoteException {
        serverRemote.sendGroupTextMessage(groupName, requireCurrentUser(), new TextMessage(content, sender()));
    }

    public void sendPrivateMessage(String receiver, String content) throws RemoteException {
        serverRemote.sendPrivateTextMessage(requireCurrentUser(), receiver, new TextMessage(content, sender()));
    }

    public String getCurrentUser() {
        return currentUser;
    }

    @Override
    public void close() throws RemoteException {
        closeCurrentSession();
    }

    private void closeCurrentSession() throws RemoteException {
        if (clientService != null) {
            clientService.close();
            clientService = null;
            currentUser = null;
        }
    }

    private User sender() {
        return new User(requireCurrentUser(), "");
    }

    private String requireCurrentUser() {
        if (currentUser == null || currentUser.isBlank()) {
            throw new IllegalStateException("Nenhum usuario autenticado.");
        }
        return currentUser;
    }

    private static ServerRemote lookupRemote() throws Exception {
        String host = getenv("WHATSUT_HOST", DEFAULT_HOST);
        int port = Integer.parseInt(getenv("WHATSUT_RMI_PORT", String.valueOf(DEFAULT_PORT)));
        String serviceName = getenv("WHATSUT_SERVICE_NAME", DEFAULT_SERVICE_NAME);
        return (ServerRemote) Naming.lookup(String.format("rmi://%s:%d/%s", host, port, serviceName));
    }

    private static String getenv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}