package com.example.Ui.View;

import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.User;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ViewSupport {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private ViewSupport() { }

    public static ListCell<Group> groupCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                    return;
                }
                String pending = group.getPendingMembers().isEmpty() ? "" : " • 🔔 " + group.getPendingMembers().size();
                setText("👥 %s\nAdmin: %s • membros: %d%s".formatted(
                        group.getName(), group.getAdmin().GetName(), group.getMembers().size(), pending));
            }
        };
    }

    public static ListCell<User> userCell(Function<User, Boolean> onlineCheck) {
        return new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    return;
                }
                setText("👤 " + user.GetName() + "  " + (Boolean.TRUE.equals(onlineCheck.apply(user)) ? "🟢" : "⚫"));
            }
        };
    }

    public static ListCell<ConversationItem> conversationCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ConversationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                if (item.type() == ConversationItem.Type.PRIVATE) {
                    setText("👤 %s  %s".formatted(item.name(), item.online() ? "🟢" : "⚫"));
                } else {
                    Group group = item.group();
                    String pending = group.getPendingMembers().isEmpty() ? "" : "  🔔 " + group.getPendingMembers().size();
                    setText("👥 %s\n%d membros%s".formatted(group.getName(), group.getMembers().size(), pending));
                }
            }
        };
    }

    public static ListCell<Message> messageCell(String currentUser, Consumer<FileMessage> downloadHandler) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setText(null);
                setGraphic(messageBubble(message, currentUser, downloadHandler));
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

    private static HBox messageBubble(Message message, String currentUser, Consumer<FileMessage> downloadHandler) {
        Label sender = new Label(message.getSender().GetName());
        sender.getStyleClass().add("message-sender");
        Label time = new Label(FORMATTER.format(message.getTimestamp()));
        time.getStyleClass().add("message-time");
        VBox bubble = new VBox(4, sender, messageContent(message, downloadHandler), time);
        bubble.getStyleClass().add("message-bubble");
        HBox wrapper = new HBox(bubble);
        boolean mine = currentUser != null && currentUser.equals(message.getSender().GetName());
        wrapper.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    private static javafx.scene.Node messageContent(Message message, Consumer<FileMessage> downloadHandler) {
        if (message instanceof TextMessage textMessage) {
            Label content = new Label(textMessage.getContent());
            content.getStyleClass().add("message-content");
            content.setWrapText(true);
            content.setMaxWidth(520);
            return content;
        }
        if (message instanceof FileMessage fileMessage) {
            VBox box = new VBox(6);
            if (isImage(fileMessage.getFileName())) {
                ImageView preview = new ImageView(new Image(new ByteArrayInputStream(fileMessage.getContent()), 260, 180, true, true));
                preview.setPreserveRatio(true);
                box.getChildren().add(preview);
            }
            Label name = new Label("📎 " + fileMessage.getFileName());
            Button download = new Button("⬇");
            download.setOnAction(event -> downloadHandler.accept(fileMessage));
            HBox row = new HBox(8, name, download);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
            return box;
        }
        return new Label("Mensagem recebida.");
    }

    private static boolean isImage(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp");
    }
}