package com.example.Ui.View;

import com.example.Models.Group;
import com.example.Ui.Controller.GroupsController;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class GroupsView {
    private final VBox root = new VBox(8);
    private final ListView<Group> groupsList = new ListView<>();

    public GroupsView(GroupsController controller, Consumer<Group> selectionHandler) {
        root.getStyleClass().add("sidebar");
        TextField groupNameField = new TextField();
        groupNameField.setPromptText("nome do grupo");
        Button createButton = new Button("Criar");
        Button joinButton = new Button("Entrar");
        Button refreshButton = new Button("Atualizar");
        refreshButton.getStyleClass().add("secondary-button");
        groupsList.setItems(controller.groups());
        groupsList.setCellFactory(list -> ViewSupport.groupCell(controller.currentUserName()));
        groupsList.getSelectionModel().selectedItemProperty().addListener((obs, old, group) -> selectionHandler.accept(group));
        createButton.setOnAction(event -> run(() -> controller.createGroup(groupNameField.getText())));
        joinButton.setOnAction(event -> run(() -> controller.joinGroup(groupsList.getSelectionModel().getSelectedItem())));
        refreshButton.setOnAction(event -> run(controller::refreshGroups));
        HBox actions = new HBox(8, createButton, joinButton, refreshButton);
        VBox header = new VBox(10, groupNameField, actions);
        header.getStyleClass().add("sidebar-header");
        VBox.setVgrow(groupsList, Priority.ALWAYS);
        root.getChildren().addAll(header, groupsList);
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
