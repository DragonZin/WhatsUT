package com.example.Ui.View;

import com.example.Ui.Controller.ChatController;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class ChatView {
    private final BorderPane root = new BorderPane();
    private final TextField messageField = new TextField();

    public ChatView(ChatController controller) {
        ListView<com.example.Models.Message> messagesList = new ListView<>(controller.messages());
        messagesList.setCellFactory(list -> ViewSupport.messageCell());
        messageField.setPromptText("mensagem");
        Button sendButton = new Button("Enviar");
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(event -> run(() -> {
            controller.sendMessage(messageField.getText());
            messageField.clear();
        }));
        root.setCenter(messagesList);
        root.setBottom(new HBox(8, messageField, sendButton));
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