package com.example.Service;

import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.FileMessage;
import com.example.Models.User;
import com.example.Rmi.WhatsUTRemote;
import com.example.Socket.WhatsUTSocket;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WhatsUTService extends UnicastRemoteObject implements WhatsUTRemote {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, User> authenticatedUsers = new ConcurrentHashMap<>();
    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final WhatsUTSocket notificationServer;

    public WhatsUTService(WhatsUTSocket notificationServer) throws RemoteException {
        super();
        this.notificationServer = notificationServer;
    }

    @Override
    public synchronized User registerUser(String name, String password) throws RemoteException {
        validateRequired(name, "Nome");
        validateRequired(password, "Password");

        if (users.containsKey(name)) {
            throw new RemoteException("Usuario ja existe: " + name);
        }

        User user = new User(name, hashPassword(password));
        users.put(name, user);
        return user;
    }

    @Override
    public boolean authenticate(String name, String password) throws RemoteException {
        validateRequired(name, "Nome");
        validateRequired(password, "Password");

        User user = users.get(name);
        if (user != null && user.VerifyHashPassword(hashPassword(password))) {
            authenticatedUsers.put(name, user);
            return true;
        }
        return false;
    }

    @Override
    public void logout(String userName) throws RemoteException {
        isAuthenticated(userName);
        authenticatedUsers.remove(userName);
    }

    @Override
    public synchronized Group createGroup(String groupName, String adminName) throws RemoteException {
        isAuthenticated(adminName);
        validateRequired(groupName, "Nome do grupo");
        User admin = getExistingUser(adminName);

        if (groups.containsKey(groupName)) {
            throw new RemoteException("Grupo ja existe: " + groupName);
        }

        Group group = new Group(groupName, admin);
        groups.put(groupName, group);
        
        notificationServer.notifyUsers(authenticatedUsers.values(), "NEW_GROUP");
        return group;
    }

    @Override
    public synchronized boolean requestJoinGroup(String groupName, String userName) throws RemoteException {
        isAuthenticated(userName);
        Group group = getExistingGroup(groupName);
        User user = getExistingUser(userName);

        if (group.hasMember(user) || group.hasPendingMember(user)) {
            return false;
        }

        group.addPendingMember(user);
        
        notificationServer.notifyUser(group.getAdmin(), "NEW_JOIN_REQUEST");
        return true;
    }

    @Override
    public synchronized boolean approvePendingMember(String groupName, String adminName, String userName) throws RemoteException {
        isAuthenticated(adminName);
        Group group = getExistingGroup(groupName);
        User admin = getExistingUser(adminName);
        User user = getExistingUser(userName);

        if (!group.getAdmin().equals(admin)) {
            throw new RemoteException("Apenas o administrador pode aprovar membros.");
        }

        return group.approvePendingMember(user);
    }

    @Override
    public synchronized boolean sendTextMessage(String groupName, String senderName, String content) throws RemoteException {
        isAuthenticated(senderName);
        validateRequired(content, "Mensagem");
        Group group = getExistingGroup(groupName);
        User sender = getExistingUser(senderName);

        if (!group.hasMember(sender)) {
            throw new RemoteException("Usuario nao pertence ao grupo: " + senderName);
        }

        group.addMessage(new TextMessage(content, sender));

        notificationServer.notifyUsers(group.getMembers(), "NEW_MESSAGE");
        return true;
    }

    @Override
    public List<Message> getMessages(String groupName, String userName) throws RemoteException {
        isAuthenticated(userName);
        Group group = getExistingGroup(groupName);
        User user = getExistingUser(userName);

        if (!group.hasMember(user)) {
            throw new RemoteException("Utilizador nao pertence ao grupo: " + userName);
        }

        return group.getMessages();
    }

    @Override
    public List<Group> listGroups(String userName) throws RemoteException {
        isAuthenticated(userName);
        return new ArrayList<>(groups.values());
    }

    @Override
    public List<User> listGroupUsers(String userName, String groupName) throws RemoteException {
        isAuthenticated(userName);
        Group group = getExistingGroup(groupName);

        if (!group.hasMember(getExistingUser(userName))) {
            throw new RemoteException("Usuario nao pertence ao grupo: " + userName);
        }

        return new ArrayList<>(group.getMembers());
    }

    @Override
    public List<User> listAuthenticatedUsers(String userName) throws RemoteException {
        isAuthenticated(userName);        
        return new ArrayList<>(authenticatedUsers.values());
    }

    private void isAuthenticated(String userName) throws RemoteException {
        validateRequired(userName, "Nome de usuario");

        if (!authenticatedUsers.containsKey(userName)) {
            throw new RemoteException("Usuario nao autenticado: " + userName);
        }
    }

    private User getExistingUser(String userName) throws RemoteException {
        validateRequired(userName, "Nome de usuario");
        User user = users.get(userName);
        if (user == null) {
            throw new RemoteException("Usuario nao encontrado: " + userName);
        }
        return user;
    }

    private Group getExistingGroup(String groupName) throws RemoteException {
        validateRequired(groupName, "Nome do grupo");
        Group group = groups.get(groupName);
        if (group == null) {
            throw new RemoteException("Grupo nao encontrado: " + groupName);
        }
        return group;
    }

    private static void validateRequired(String value, String fieldName) throws RemoteException {
        if (value == null || value.isBlank()) {
            throw new RemoteException(fieldName + " e obrigatorio.");
        }
    }

    private static String hashPassword(String password) throws RemoteException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new RemoteException("Nao foi possivel gerar hash da password.", exception);
        }
    }
}