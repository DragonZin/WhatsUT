package com.example.Ui;

import com.example.Ui.Controller.ChatController;
import com.example.Ui.Controller.GroupsController;
import com.example.Ui.Controller.LoginController;
import com.example.Ui.Controller.UsersController;
import com.example.Ui.Service.RmiClientService;
import com.example.Ui.Service.UiCallbackHandler;
import com.example.Ui.View.ChatView;
import com.example.Ui.View.GroupsView;
import com.example.Ui.View.UsersView;
import com.example.Ui.View.LoginView;
import com.example.Ui.View.ViewSupport;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    private RmiClientService rmiClientService;

    @Override
    public void start(Stage stage) {
        try {
            UiCallbackHandler callbackHandler = new UiCallbackHandler();
            rmiClientService = new RmiClientService(callbackHandler);
            LoginController loginController = new LoginController(rmiClientService);
            loginController.onLoginSuccess(userName -> showMainScene(stage, callbackHandler));
            stage.setTitle("WhatsUT");
            stage.setScene(styledScene(new LoginView(loginController).root(), 480, 420));
            stage.show();
        } catch (Exception exception) {
            ViewSupport.showError(exception);
        }
    }

    @Override
    public void stop() throws Exception {
        if (rmiClientService != null) {
            rmiClientService.close();
        }
    }

    private void showMainScene(Stage stage, UiCallbackHandler callbackHandler) {
        GroupsController groupsController = new GroupsController(rmiClientService);
        ChatController chatController = new ChatController(rmiClientService);
        UsersController usersController = new UsersController(rmiClientService);
        GroupsView groupsView = new GroupsView(groupsController, group -> run(() -> chatController.selectGroup(group)));
        UsersView usersView = new UsersView(usersController, userName -> run(() -> chatController.selectPrivateUser(userName)));
        ChatView chatView = new ChatView(chatController);
        configureCallbacks(callbackHandler, groupsController, chatController, usersController);
        run(groupsController::refreshGroups);
        run(usersController::refreshOnlineUsers);
        TabPane sideTabs = new TabPane(new Tab("Grupos", groupsView.root()), new Tab("Usuarios", usersView.root()));
        sideTabs.getTabs().forEach(tab -> tab.setClosable(false));
        SplitPane splitPane = new SplitPane(sideTabs, chatView.root());
        splitPane.setDividerPositions(0.36);
        stage.setScene(styledScene(splitPane, 1040, 680));
    }

    private static Scene styledScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/styles/whatsut.css").toExternalForm());
        return scene;
    }

    private static void configureCallbacks(UiCallbackHandler callbackHandler, GroupsController groupsController,
            ChatController chatController, UsersController usersController) {
        callbackHandler.onGroupsRefresh(() -> run(groupsController::refreshGroups));
        callbackHandler.onJoinRequest((groupName, requesterName) -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Aprovar %s no grupo %s?".formatted(requesterName, groupName));
            alert.showAndWait().ifPresent(response -> run(() -> groupsController.approveMember(groupName, requesterName)));
        });
        callbackHandler.onGroupMessage(chatController::appendGroupMessage);
        callbackHandler.onPrivateMessage(chatController::appendPrivateMessage);
        callbackHandler.onError(ViewSupport::showError);
        callbackHandler.onGroupsRefresh(() -> {
            run(groupsController::refreshGroups);
            run(usersController::refreshOnlineUsers);
        });
    }

    @FunctionalInterface
    private interface RemoteUiAction {
        void run() throws Exception;
    }

    private static void run(RemoteUiAction action) {
        try {
            action.run();
        } catch (Exception exception) {
            ViewSupport.showError(exception);
        }
    }
}