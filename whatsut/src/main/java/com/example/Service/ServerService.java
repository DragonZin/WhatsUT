package com.example.Service;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.PrivateMessages;
import com.example.Models.TextMessage;
import com.example.Models.User;
import com.example.Rmi.ClientRemote;
import com.example.Rmi.ServerRemote;
import com.example.Utils.ConversationKey;

public class ServerService extends UnicastRemoteObject implements ServerRemote {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, User> authenticatedUsers = new ConcurrentHashMap<>();
    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final Map<ConversationKey, PrivateMessages> privateMessages = new ConcurrentHashMap<>();
    private final Map<String, ClientRemote> clientCallbacks = new ConcurrentHashMap<>();
    private final ExecutorService callbackExecutor = Executors.newCachedThreadPool();

    public ServerService() throws RemoteException {
        super();
    }

    @Override
    public synchronized User registerUser(String userName, String password) throws RemoteException {
        validateRequired(userName, "Nome");
        validateRequired(password, "Password");

        if (users.containsKey(userName)) {
            throw new RemoteException("Usuario ja existe: " + userName);
        }

        User user = new User(userName, hashPassword(password));
        users.put(userName, user);
        return user;
    }

    @Override
    public synchronized boolean login(String userName, String password, ClientRemote clientRemote) throws RemoteException {
        validateRequired(userName, "Nome");
        validateRequired(password, "Password");
        
        if (clientRemote == null) {
            throw new RemoteException("Callback do cliente e obrigatorio.");
        }

        User user = users.get(userName);
        if (user == null || !user.VerifyHashPassword(hashPassword(password))) {
            throw new RemoteException("Credenciais invalidas para usuario: " + userName);
        }

        authenticatedUsers.put(userName, user);
        clientCallbacks.put(userName, clientRemote);
        return true;
    }

    @Override
    public synchronized void logout(String userName) throws RemoteException {
        isAuthenticated(userName);
        authenticatedUsers.remove(userName);
        clientCallbacks.remove(userName);
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

        return group.addPendingMember(user);
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

    @Deprecated
    public synchronized boolean deleteUserFromGroup(String groupName, String adminName, String userName) throws RemoteException {
        isAuthenticated(adminName);
        Group group = getExistingGroup(groupName);
        User admin = getExistingUser(adminName);
        User user = getExistingUser(userName);

        if (!group.getAdmin().equals(admin)) {
            throw new RemoteException("Apenas o administrador pode remover membros.");
        }

        if (adminName.equals(userName)) {
            groups.remove(groupName);
        }

        return group.removeMember(user);
    }

    @Override
    public synchronized boolean sendPrivateTextMessage(String Sender, String Receiver, TextMessage textMessage) throws RemoteException {
        isAuthenticated(Sender);
        validateRequired(textMessage.getContent(), "Mensagem");
        getExistingUser(Receiver);

        ConversationKey key = new ConversationKey(Sender, Receiver);
        
        privateMessages.computeIfAbsent(key, k -> new PrivateMessages(Sender, Receiver)).addMessage(textMessage);
        return true;
    }

    @Override
    public synchronized boolean sendPrivateFileMessage(String Sender, String Receiver, FileMessage fileMessage) throws RemoteException {
        isAuthenticated(Sender);
        validateRequired(fileMessage.getFileName(), "Nome do arquivo");
        getExistingUser(Receiver);

        ConversationKey key = new ConversationKey(Sender, Receiver);

        privateMessages.computeIfAbsent(key, k -> new PrivateMessages(Sender, Receiver)).addMessage(fileMessage);

        return true;
    }

    @Override
    public synchronized boolean sendGroupTextMessage(String groupName, String senderName, TextMessage textMessage) throws RemoteException {
        isAuthenticated(senderName);
        validateRequired(textMessage.getContent(), "Mensagem");
        Group group = getExistingGroup(groupName);
        User sender = getExistingUser(senderName);

        if (!group.hasMember(sender)) {
            throw new RemoteException("Usuario nao pertence ao grupo: " + senderName);
        }

        group.addMessage(textMessage);

        return true;
    }

    @Override
    public synchronized boolean sendGroupFileMessage(String groupName, String senderName, FileMessage fileMessage) throws RemoteException {
        isAuthenticated(senderName);
        validateRequired(fileMessage.getFileName(), "Nome do arquivo");
        Group group = getExistingGroup(groupName);
        User sender = getExistingUser(senderName);

        if (!group.hasMember(sender)) {
            throw new RemoteException("Usuario nao pertence ao grupo: " + senderName);
        }

        group.addMessage(fileMessage);

        return true;
    }

    @Override
    public List<Message> getPrivateMessages(String Sender, String Receiver) throws RemoteException {
        isAuthenticated(Sender);
        ConversationKey key = new ConversationKey(Sender, Receiver);
        PrivateMessages messages = privateMessages.get(key);

        return messages.getMessages();
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