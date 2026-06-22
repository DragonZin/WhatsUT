package com.example.Ui.View;

import com.example.Ui.Controller.LoginController;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class LoginView {
    private final VBox root = new VBox();
    private final TextField userNameField = new TextField();
    private final PasswordField passwordField = new PasswordField();

    public LoginView(LoginController controller) {
        root.getStyleClass().add("login-root");
        root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("login-card");

        Label title = new Label("WhatsUT");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Converse com grupos e usuarios em tempo real");
        subtitle.getStyleClass().add("muted-label");

        userNameField.setPromptText("usuario");
        passwordField.setPromptText("password");
        Button registerButton = new Button("Registar");
        Button loginButton = new Button("Entrar");
        registerButton.getStyleClass().add("secondary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(registerButton, Priority.ALWAYS);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        registerButton.setOnAction(event -> run(() -> {
            controller.register(userNameField.getText(), passwordField.getText());
            ViewSupport.showInfo("Usuario registado.");
        }));
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> run(() -> controller.login(userNameField.getText(), passwordField.getText())));
        card.getChildren().addAll(title, subtitle, userNameField, passwordField, new HBox(10, registerButton, loginButton));
        root.getChildren().add(card);
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