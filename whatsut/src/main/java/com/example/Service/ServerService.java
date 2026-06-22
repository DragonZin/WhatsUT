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

        if (authenticatedUsers.containsKey(userName)) {
            throw new RemoteException("Usuario ja autenticado: " + userName);
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

        Group group = new Group(groupName.trim(), admin);
        groups.put(group.getName(), group);
        notifyAllGroupsChanged();
        notifyGroupCreated(adminName, group.getName());

        return group;
    }

    @Override
    public synchronized Group createGroup(String groupName, String description, String adminName, List<String> memberNames) throws RemoteException {
        isAuthenticated(adminName);
        validateRequired(groupName, "Nome do grupo");
        User admin = getExistingUser(adminName);

        if (groups.containsKey(groupName)) {
            throw new RemoteException("Grupo ja existe: " + groupName);
        }

        if (memberNames == null || memberNames.isEmpty()) {
            throw new RemoteException("Selecione ao menos um participante alem do administrador.");
        }

        List<User> initialMembers = new ArrayList<>();
        for (String memberName : memberNames) {
            validateRequired(memberName, "Usuario");
            if (memberName.equals(adminName)) {
                continue;
            }
            User member = getExistingUser(memberName);
            if (initialMembers.contains(member)) {
                throw new RemoteException("Usuario duplicado: " + memberName);
            }
            initialMembers.add(member);
        }
        if (initialMembers.isEmpty()) {
            throw new RemoteException("Selecione ao menos um participante alem do administrador.");
        }

        Group group = new Group(groupName.trim(), description, admin, initialMembers);
        groups.put(group.getName(), group);
        notifyAllGroupsChanged();
        group.getMembers().forEach(member -> notifyGroupCreated(member.GetName(), group.getName()));

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

        boolean added = group.addPendingMember(user);
        if (added) {
            notifyJoinRequest(group, userName);
        }

        return added;
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

        boolean approved = group.approvePendingMember(user);
        if (approved) {
            notifyJoinApproved(group, userName);
            notifyGroupMembersChanged(group);
        }

        return approved;
    }

    @Override
    public synchronized boolean rejectPendingMember(String groupName, String adminName, String userName) throws RemoteException {
        isAuthenticated(adminName);
        Group group = getExistingGroup(groupName);
        User admin = getExistingUser(adminName);
        User user = getExistingUser(userName);

        if (!group.getAdmin().equals(admin)) {
            throw new RemoteException("Apenas o administrador pode recusar solicitacoes.");
        }

        boolean rejected = group.rejectPendingMember(user);
        if (rejected) {
            notifyGroupsChanged(adminName);
            notifyGroupsChanged(userName);
        }
        return rejected;
    }

    @Override
    public synchronized boolean cancelJoinRequest(String groupName, String userName) throws RemoteException {
        isAuthenticated(userName);
        Group group = getExistingGroup(groupName);
        User user = getExistingUser(userName);
        boolean canceled = group.cancelPendingMember(user);
        if (canceled) {
            notifyGroupsChanged(group.getAdmin().GetName());
            notifyGroupsChanged(userName);
        }
        return canceled;
    }

    @Override
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

        boolean removed = group.removeMember(user);
        if (removed) {
            notifyParticipantRemoved(group, userName);
        }
        return removed;
    }

    @Override
    public synchronized boolean sendPrivateTextMessage(String Sender, String Receiver, TextMessage textMessage) throws RemoteException {
        isAuthenticated(Sender);
        validateRequired(textMessage.getContent(), "Mensagem");
        getExistingUser(Receiver);

        ConversationKey key = new ConversationKey(Sender, Receiver);
        
        privateMessages.computeIfAbsent(key, k -> new PrivateMessages(Sender, Receiver)).addMessage(textMessage);
        notifyPrivateMessage(Sender, Receiver, textMessage);

        return true;
    }

    @Override
    public synchronized boolean sendPrivateFileMessage(String Sender, String Receiver, FileMessage fileMessage) throws RemoteException {
        isAuthenticated(Sender);
        validateFileMessage(fileMessage);
        getExistingUser(Receiver);

        ConversationKey key = new ConversationKey(Sender, Receiver);

        privateMessages.computeIfAbsent(key, k -> new PrivateMessages(Sender, Receiver)).addMessage(fileMessage);
        notifyPrivateMessage(Sender, Receiver, fileMessage);

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
        notifyGroupMessage(group, textMessage);

        return true;
    }

    @Override
    public synchronized boolean sendGroupFileMessage(String groupName, String senderName, FileMessage fileMessage) throws RemoteException {
        isAuthenticated(senderName);
        validateFileMessage(fileMessage);
        Group group = getExistingGroup(groupName);
        User sender = getExistingUser(senderName);

        if (!group.hasMember(sender)) {
            throw new RemoteException("Usuario nao pertence ao grupo: " + senderName);
        }

        group.addMessage(fileMessage);
        notifyGroupMessage(group, fileMessage);
        
        return true;
    }

    @Override
    public List<Message> getPrivateMessages(String Sender, String Receiver) throws RemoteException {
        isAuthenticated(Sender);
        ConversationKey key = new ConversationKey(Sender, Receiver);
        PrivateMessages messages = privateMessages.get(key);
        if (messages == null) {
            return List.of();
        }
        
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
    public List<User> listUsers(String userName) throws RemoteException {
        isAuthenticated(userName);
        return new ArrayList<>(users.values());
    }

    @Override
    public List<User> listAuthenticatedUsers(String userName) throws RemoteException {
        isAuthenticated(userName);        
        return new ArrayList<>(authenticatedUsers.values());
    }

    private void notifyAllGroupsChanged() {
        clientCallbacks.entrySet().forEach(entry -> {
            String userName = entry.getKey();
            ClientRemote client = entry.getValue();

            callbackExecutor.submit(() -> {
                try {
                    client.refreshGroups();
                } catch (RemoteException e) {
                    removeDeadClient(userName);
                }
            });
        });
    }

    private void notifyGroupMembersChanged(Group group) {
        group.getMembers().forEach(member -> notifyGroupsChanged(member.GetName()));
    }

    private void notifyGroupsChanged(String userName) {
        ClientRemote client = clientCallbacks.get(userName);
        if (client == null) {
            return;
        }

        callbackExecutor.submit(() -> {
            try {
                client.refreshGroups();
            } catch (RemoteException e) {
                removeDeadClient(userName);
            }
        });
    }

    private void notifyJoinRequest(Group group, String requesterName) {
        ClientRemote adminClient = clientCallbacks.get(group.getAdmin().GetName());
        if (adminClient == null) {
            return;
        }

        callbackExecutor.submit(() -> {
            try {
                adminClient.refreshRequest(group.getName(), requesterName);
            } catch (RemoteException e) {
                removeDeadClient(group.getAdmin().GetName());
            }
        });
    }

    private void notifyGroupMessage(Group group, Message message) {
        group.getMembers().forEach(member -> {
            ClientRemote client = clientCallbacks.get(member.GetName());
            if (client == null) {
                return;
            }

            callbackExecutor.submit(() -> {
                try {
                    client.refreshMessage(group.getName(), message);
                } catch (RemoteException e) {
                    removeDeadClient(member.GetName());
                }
            });
        });
    }

    private void notifyGroupCreated(String userName, String groupName) {
        notifyClient(userName, client -> client.onGroupCreated(groupName));
    }

    private void notifyJoinApproved(Group group, String userName) {
        notifyClient(userName, client -> client.onGroupJoinApproved(group.getName()));
        group.getMembers().forEach(member -> notifyClient(member.GetName(), client -> client.onParticipantAdded(group.getName(), userName)));
    }

    private void notifyParticipantRemoved(Group group, String userName) {
        notifyClient(userName, client -> client.onParticipantRemoved(group.getName(), userName));
        group.getMembers().forEach(member -> notifyClient(member.GetName(), client -> client.onParticipantRemoved(group.getName(), userName)));
    }

    private void notifyClient(String userName, RemoteClientAction action) {
        ClientRemote client = clientCallbacks.get(userName);
        if (client == null) {
            return;
        }
        callbackExecutor.submit(() -> {
            try {
                action.accept(client);
            } catch (RemoteException e) {
                removeDeadClient(userName);
            }
        });
    }

    @FunctionalInterface
    private interface RemoteClientAction {
        void accept(ClientRemote client) throws RemoteException;
    }

    private void notifyPrivateMessage(String senderName, String receiverName, Message message) {
        ClientRemote receiverClient = clientCallbacks.get(receiverName);
        if (receiverClient == null) {
            return;
        }

        callbackExecutor.submit(() -> {
            try {
                receiverClient.refreshPrivateMessage(senderName, message);
            } catch (RemoteException e) {
                removeDeadClient(receiverName);
            }
        });
    }

    private void isAuthenticated(String userName) throws RemoteException {
        validateRequired(userName, "Nome de usuario");

        if (!authenticatedUsers.containsKey(userName)) {
            throw new RemoteException("Usuario nao autenticado: " + userName);
        }
    }

    private void removeDeadClient(String userName) {
        authenticatedUsers.remove(userName);
        clientCallbacks.remove(userName);
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

    private static void validateFileMessage(FileMessage fileMessage) throws RemoteException {
        if (fileMessage == null) {
            throw new RemoteException("Arquivo e obrigatorio.");
        }

        validateRequired(fileMessage.getFileName(), "Nome do arquivo");
        if (fileMessage.getSize() == 0) {
            throw new RemoteException("Arquivo vazio nao pode ser enviado.");
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