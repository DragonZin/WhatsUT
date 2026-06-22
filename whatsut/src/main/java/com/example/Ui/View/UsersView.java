package com.example.Ui.View;

import com.example.Models.User;
import com.example.Ui.Controller.UsersController;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class UsersView {
    private final VBox root = new VBox(8);

    public UsersView(UsersController controller, Consumer<String> selectionHandler) {
        root.getStyleClass().add("sidebar");
        ListView<User> usersList = new ListView<>(controller.onlineUsers());
        usersList.setCellFactory(list -> ViewSupport.userCell());
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> {
            if (user != null) {
                selectionHandler.accept(user.GetName());
            }
        });
        Button refreshButton = new Button("Atualizar online");
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        refreshButton.setOnAction(event -> {
            try {
                controller.refreshOnlineUsers();
            } catch (Exception exception) {
                ViewSupport.showError(exception);
            }
        });
        VBox header = new VBox(8, styledTitle("Usuarios online"), refreshButton);
        header.getStyleClass().add("sidebar-header");
        VBox.setVgrow(usersList, Priority.ALWAYS);
        root.getChildren().addAll(header, usersList);
    }

    public Parent root() {
        return root;
    }

    private static Label styledTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }
}