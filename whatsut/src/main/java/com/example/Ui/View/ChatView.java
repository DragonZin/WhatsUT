package com.example.Ui.View;

import com.example.Ui.Controller.ChatController;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ChatView {
    private final BorderPane root = new BorderPane();
    private final TextField messageField = new TextField();

    public ChatView(ChatController controller) {
        root.getStyleClass().add("chat-root");
        Label title = new Label("Conversa");
        title.getStyleClass().add("chat-title");
        Label subtitle = new Label("Selecione um usuario ou grupo para iniciar");
        subtitle.getStyleClass().add("chat-subtitle");
        root.setTop(new VBox(2, title, subtitle));
        root.getTop().getStyleClass().add("chat-header");

        ListView<com.example.Models.Message> messagesList = new ListView<>(controller.messages());
        messagesList.getStyleClass().add("message-list");
        messagesList.setCellFactory(list -> ViewSupport.messageCell());
        messageField.setPromptText("Digite uma mensagem");
        Button sendButton = new Button("Enviar");
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(event -> run(() -> {
            controller.sendMessage(messageField.getText());
            messageField.clear();
        }));
        HBox composer = new HBox(10, messageField, sendButton);
        composer.getStyleClass().add("composer");
        composer.setPadding(new Insets(12, 14, 12, 14));
        HBox.setHgrow(messageField, Priority.ALWAYS);
        root.setCenter(messagesList);
        root.setBottom(composer);
    }

    public Parent root() {
        return root;
    }

    private static void run(RemoteUiAction action) {
        try {
            action.run();
        } catch (Exception exception) {
            ViewSupport.showError(exception);
        }
    }

    private interface RemoteUiAction {
        void run() throws Exception;
    }
}