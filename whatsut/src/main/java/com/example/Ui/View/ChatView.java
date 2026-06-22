package com.example.Ui.View;

import com.example.Ui.Controller.ChatController;
import com.example.Models.Group;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Consumer;

public class ChatView {
    private final BorderPane root = new BorderPane();
    private final TextField messageField = new TextField();

    public ChatView(ChatController controller, Consumer<Group> pendingRequestsAction) {
        root.getStyleClass().add("chat-root");
        Label title = new Label("Conversa");
        title.getStyleClass().add("chat-title");
        title.textProperty().bind(controller.conversationTitleProperty());
        Label subtitle = new Label("Selecione um usuario ou grupo para iniciar");
        subtitle.getStyleClass().add("chat-subtitle");
        subtitle.textProperty().bind(controller.conversationSubtitleProperty());
        VBox conversationDetails = new VBox(2, title, subtitle);
        Button pendingRequestsButton = new Button("Solicitacoes");
        pendingRequestsButton.getStyleClass().add("pending-requests-button");
        pendingRequestsButton.visibleProperty().bind(controller.selectedGroupAdminProperty());
        pendingRequestsButton.managedProperty().bind(controller.selectedGroupAdminProperty());
        pendingRequestsButton.setOnAction(event -> {
            Group group = controller.selectedGroup();
            if (group != null) {
                pendingRequestsAction.accept(group);
            }
        });
        HBox headerContent = new HBox(12, conversationDetails, pendingRequestsButton);
        HBox.setHgrow(conversationDetails, Priority.ALWAYS);
        headerContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        root.setTop(headerContent);
        root.getTop().getStyleClass().add("chat-header");

        ListView<com.example.Models.Message> messagesList = new ListView<>(controller.messages());
        Button recentButton = new Button("Ir para mensagens recentes");
        recentButton.getStyleClass().add("secondary-button");
        recentButton.setVisible(false);
        recentButton.setManaged(false);
        recentButton.setOnAction(event -> {
            if (!controller.messages().isEmpty()) {
                messagesList.scrollTo(controller.messages().size() - 1);
                recentButton.setVisible(false);
                recentButton.setManaged(false);
            }
        });
        controller.messages().addListener((javafx.collections.ListChangeListener<com.example.Models.Message>) change -> Platform.runLater(() -> {
            if (controller.messages().isEmpty()) {
                return;
            }
            int lastVisible = messagesList.getSelectionModel().getSelectedIndex();
            boolean nearEnd = lastVisible < 0 || lastVisible >= controller.messages().size() - 4;
            if (nearEnd) {
                messagesList.scrollTo(controller.messages().size() - 1);
            } else {
                recentButton.setVisible(true);
                recentButton.setManaged(true);
            }
        }));
        messagesList.getStyleClass().add("message-list");
        messagesList.setCellFactory(list -> ViewSupport.messageCell(controller.currentUserName(), fileMessage -> run(() -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Selecionar pasta para salvar");
            File directory = chooser.showDialog(root.getScene().getWindow());
            if (directory != null) {
                java.nio.file.Path savedFile = controller.downloadFile(fileMessage, directory.toPath());
                new Alert(Alert.AlertType.INFORMATION, "Arquivo salvo em: " + savedFile).showAndWait();
            }
        })));
        messageField.setPromptText("Digite uma mensagem");
        Button attachButton = new Button("Anexar");
        attachButton.setOnAction(event -> run(() -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selecionar arquivo para enviar");
            File file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                controller.sendFile(file.toPath());
            }
        }));
        Button sendButton = new Button("Enviar");
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(event -> run(() -> {
            controller.sendMessage(messageField.getText());
            messageField.clear();
        }));
        HBox composer = new HBox(10, attachButton, messageField, sendButton);
        composer.getStyleClass().add("composer");
        composer.setPadding(new Insets(12, 14, 12, 14));
        HBox.setHgrow(messageField, Priority.ALWAYS);
        root.setCenter(new VBox(messagesList, recentButton));
        VBox.setVgrow(messagesList, Priority.ALWAYS);
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
