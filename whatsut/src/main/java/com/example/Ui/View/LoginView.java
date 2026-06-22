package com.example.Ui.View;

import com.example.Ui.Controller.LoginController;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class LoginView {
    private final GridPane root = new GridPane();
    private final TextField userNameField = new TextField();
    private final PasswordField passwordField = new PasswordField();

    public LoginView(LoginController controller) {
        root.setPadding(new Insets(24));
        root.setHgap(8);
        root.setVgap(12);
        userNameField.setPromptText("usuario");
        passwordField.setPromptText("password");
        Button registerButton = new Button("Registar");
        Button loginButton = new Button("Entrar");
        registerButton.setOnAction(event -> run(() -> {
            controller.register(userNameField.getText(), passwordField.getText());
            ViewSupport.showInfo("Usuario registado.");
        }));
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> run(() -> controller.login(userNameField.getText(), passwordField.getText())));
        root.addRow(0, new Label("Usuario"), userNameField);
        root.addRow(1, new Label("Password"), passwordField);
        root.add(new HBox(8, registerButton, loginButton), 1, 2);
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