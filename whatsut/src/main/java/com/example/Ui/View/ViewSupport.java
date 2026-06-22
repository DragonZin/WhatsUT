package com.example.Ui.View;

import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.User;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
                setText(empty || group == null ? null : "💬 %s\nAdmin: %s • membros: %d • pendentes: %d".formatted(
                        group.getName(), group.getAdmin().GetName(), group.getMembers().size(), group.getPendingMembers().size()));
            }
        };
    }

    static ListCell<User> userCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : "🟢 " + user.GetName());
            }
        };
    }

    static ListCell<Message> messageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(messageBubble(message));
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

    private static HBox messageBubble(Message message) {
        Label sender = new Label(message.getSender().GetName());
        sender.getStyleClass().add("message-sender");
        Label content = new Label(messageContent(message));
        content.getStyleClass().add("message-content");
        content.setMaxWidth(520);
        Label time = new Label(FORMATTER.format(message.getTimestamp()));
        time.getStyleClass().add("message-time");
        VBox bubble = new VBox(3, sender, content, time);
        bubble.getStyleClass().add("message-bubble");
        HBox wrapper = new HBox(bubble);
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    private static String messageContent(Message message) {
        if (message instanceof TextMessage textMessage) {
            return textMessage.getContent();
        }
        if (message instanceof FileMessage fileMessage) {
            return "📎 Arquivo: " + fileMessage.getFileName();
        }
        return "Mensagem recebida.";
    }
}
