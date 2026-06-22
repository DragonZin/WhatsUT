package com.example.Ui.View;

import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.User;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ViewSupport {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private ViewSupport() { }

    static ListCell<Group> groupCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                setText(empty || group == null ? null : "%s (admin: %s, membros: %d, pendentes: %d)".formatted(
                        group.getName(), group.getAdmin().GetName(), group.getMembers().size(), group.getPendingMembers().size()));
            }
        };
    }

    static ListCell<User> userCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.GetName());
            }
        };
    }

    static ListCell<Message> messageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                setText(empty || message == null ? null : formatMessage(message));
            }
        };
    }

    public static void showError(Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("WhatsUT");
        alert.setHeaderText("Operacao nao concluida");
        alert.setContentText(throwable.getMessage());
        alert.showAndWait();
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle("WhatsUT");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static String formatMessage(Message message) {
        String sender = message.getSender().GetName();
        String time = FORMATTER.format(message.getTimestamp());
        if (message instanceof TextMessage textMessage) {
            return "[%s] %s: %s".formatted(time, sender, textMessage.getContent());
        }
        if (message instanceof FileMessage fileMessage) {
            return "[%s] %s enviou arquivo: %s".formatted(time, sender, fileMessage.getFileName());
        }
        return "[%s] %s enviou uma mensagem.".formatted(time, sender);
    }
}