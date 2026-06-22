package com.example.Ui.Controller;

import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Ui.Service.RmiClientService;
import javafx.collections.FXCollections;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;

public class ChatController {
    private final RmiClientService rmiClientService;
    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private Group selectedGroup;
    private String selectedPrivateUser;
    private final SimpleStringProperty conversationTitle = new SimpleStringProperty("Conversa");
    private final SimpleStringProperty conversationSubtitle = new SimpleStringProperty("Selecione um usuario ou grupo para iniciar");

    public ChatController(RmiClientService rmiClientService) {
        this.rmiClientService = rmiClientService;
    }

    public ObservableList<Message> messages() {
        return messages;
    }

    public ReadOnlyStringProperty conversationTitleProperty() {
        return conversationTitle;
    }

    public ReadOnlyStringProperty conversationSubtitleProperty() {
        return conversationSubtitle;
    }

    public String currentUserName() {
        return rmiClientService.getCurrentUser();
    }

        public String currentConversationKey() {
        if (selectedGroup != null) {
            return "G:" + selectedGroup.getName();
        }
        if (selectedPrivateUser != null) {
            return "P:" + selectedPrivateUser;
        }
        return "";
    }

    public boolean isGroupOpen(String groupName) {
        return selectedGroup != null && selectedGroup.getName().equals(groupName);
    }

    public boolean isPrivateOpen(String userName) {
        return selectedPrivateUser != null && selectedPrivateUser.equals(userName);
    }

    public void showPlaceholder(String title, String subtitle) {
        selectedGroup = null;
        selectedPrivateUser = null;
        messages.clear();
        conversationTitle.set(title);
        conversationSubtitle.set(subtitle);
    }

    public void selectGroup(Group group) throws RemoteException {
        selectedGroup = group;
        selectedPrivateUser = null;
        conversationTitle.set(group.getName());
        conversationSubtitle.set("Grupo • " + group.getMembers().size() + " membros");
        refreshMessages();
    }

    public void selectPrivateUser(String userName) throws RemoteException {
        selectedPrivateUser = userName;
        selectedGroup = null;
        conversationTitle.set(userName);
        conversationSubtitle.set("Conversa privada");
        refreshMessages();
    }

    public void refreshMessages() throws RemoteException {
        if (selectedGroup != null) {
            messages.setAll(rmiClientService.getGroupMessages(selectedGroup.getName()));
        } else if (selectedPrivateUser != null) {
            messages.setAll(rmiClientService.getPrivateMessages(selectedPrivateUser));
        } else {
            messages.clear();
        }
    }

    public void appendGroupMessage(String groupName, Message message) {
        if (selectedGroup != null && selectedGroup.getName().equals(groupName)) {
            messages.add(message);
        }
    }

    public void appendPrivateMessage(String senderName, Message message) {
        if (selectedPrivateUser != null && selectedPrivateUser.equals(senderName)) {
            messages.add(message);
        }
    }

    public void sendMessage(String content) throws RemoteException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Digite uma mensagem.");
        }
        if (selectedGroup != null) {
            rmiClientService.sendGroupMessage(selectedGroup.getName(), content.trim());
        } else if (selectedPrivateUser != null) {
            rmiClientService.sendPrivateMessage(selectedPrivateUser, content.trim());
        } else {
            throw new IllegalArgumentException("Selecione um grupo ou usuario.");
        }
    }

    public void sendFile(Path filePath) throws RemoteException {
        if (selectedGroup != null) {
            rmiClientService.sendGroupFile(selectedGroup.getName(), filePath);
        } else if (selectedPrivateUser != null) {
            rmiClientService.sendPrivateFile(selectedPrivateUser, filePath);
        } else {
            throw new IllegalArgumentException("Selecione um grupo ou usuario.");
        }
    }

    public Path downloadFile(Message message, Path destinationDirectory) throws IOException {
        if (!(message instanceof FileMessage fileMessage)) {
            throw new IllegalArgumentException("Selecione uma mensagem com arquivo.");
        }
        if (destinationDirectory == null) {
            throw new IllegalArgumentException("Selecione uma pasta de destino.");
        }

        Files.createDirectories(destinationDirectory);
        Path destination = uniqueDestination(destinationDirectory, fileMessage.getFileName());
        Files.copy(new ByteArrayInputStream(fileMessage.getContent()), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    private Path uniqueDestination(Path destinationDirectory, String fileName) {
        Path destination = destinationDirectory.resolve(fileName);
        if (!Files.exists(destination)) {
            return destination;
        }

        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        do {
            destination = destinationDirectory.resolve("%s (%d)%s".formatted(baseName, counter, extension));
            counter++;
        } while (Files.exists(destination));

        return destination;
    }
}