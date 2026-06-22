package com.example.Ui;

import com.example.Models.Group;
import com.example.Models.User;
import com.example.Ui.Controller.ChatController;
import com.example.Ui.Controller.GroupsController;
import com.example.Ui.Controller.LoginController;
import com.example.Ui.Controller.UsersController;
import com.example.Ui.Service.RmiClientService;
import com.example.Ui.Service.UiCallbackHandler;
import com.example.Ui.View.ChatView;
import com.example.Ui.View.ConversationItem;
import com.example.Ui.View.LoginView;
import com.example.Ui.View.ViewSupport;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

public class MainApp extends Application {
    private RmiClientService rmiClientService;
    private final ObservableList<ConversationItem> conversations = FXCollections.observableArrayList();

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
        ChatView chatView = new ChatView(chatController);
        BorderPane center = new BorderPane(chatView.root());
        Parent sidebar = conversationsSidebar(groupsController, usersController, chatController, center, chatView.root());
        configureCallbacks(callbackHandler, groupsController, chatController, usersController, () -> refreshAll(groupsController, usersController));
        run(() -> refreshAll(groupsController, usersController));
        SplitPane splitPane = new SplitPane(sidebar, center);
        splitPane.setDividerPositions(0.36);
        stage.setScene(styledScene(splitPane, 1040, 680));
    }

    private Parent conversationsSidebar(GroupsController groupsController, UsersController usersController,
            ChatController chatController, BorderPane center, Parent chatRoot) {
        ListView<ConversationItem> list = new ListView<>(conversations);
        list.setCellFactory(view -> ViewSupport.conversationCell());
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item == null) {
                return;
            }
            center.setCenter(chatRoot);
            if (item.type() == ConversationItem.Type.PRIVATE) {
                run(() -> chatController.selectPrivateUser(item.user().GetName()));
            } else if (isMember(item.group(), rmiClientService.getCurrentUser())) {
                run(() -> chatController.selectGroup(item.group()));
            } else {
                chatController.showPlaceholder(item.group().getName(), "Voce ainda nao participa deste grupo");
                center.setCenter(joinRequestPanel(groupsController, item.group()));
            }
        });
        Button createGroup = new Button("Criar grupo");
        createGroup.setMaxWidth(Double.MAX_VALUE);
        createGroup.setOnAction(event -> center.setCenter(createGroupPanel(groupsController, usersController, chatRoot, center)));
        Button pending = new Button("Solicitacoes pendentes");
        pending.setMaxWidth(Double.MAX_VALUE);
        pending.setOnAction(event -> {
            ConversationItem item = list.getSelectionModel().getSelectedItem();
            if (item != null && item.type() == ConversationItem.Type.GROUP) {
                center.setCenter(pendingRequestsPanel(groupsController, item.group()));
            }
        });
        Button refresh = new Button("Atualizar");
        refresh.setMaxWidth(Double.MAX_VALUE);
        refresh.setOnAction(event -> run(() -> refreshAll(groupsController, usersController)));
        VBox sidebar = new VBox(8, new Label("Conversas"), createGroup, pending, refresh, list);
        sidebar.getStyleClass().add("sidebar");
        VBox.setVgrow(list, Priority.ALWAYS);
        return sidebar;
    }

    private Parent joinRequestPanel(GroupsController controller, Group group) {
        boolean pending = isPending(group, rmiClientService.getCurrentUser());
        Label title = new Label(group.getName());
        Label status = new Label(pending ? "Solicitacao enviada" : "Entre no grupo para conversar.");
        Button join = new Button(pending ? "Solicitacao enviada" : "Pedir para entrar");
        join.setDisable(pending);
        join.setOnAction(event -> run(() -> controller.joinGroup(group)));
        Button cancel = new Button("Cancelar");
        cancel.setOnAction(event -> { });
        VBox panel = new VBox(10, title, status, new HBox(8, join, cancel));
        panel.getStyleClass().add("chat-header");
        return panel;
    }


    private Parent pendingRequestsPanel(GroupsController controller, Group group) {
        VBox rows = new VBox(8);
        if (group.getPendingMembers().isEmpty()) {
            rows.getChildren().add(new Label("Nao ha solicitacoes pendentes."));
        } else {
            for (User user : group.getPendingMembers()) {
                Button approve = new Button("Aprovar");
                approve.setOnAction(event -> run(() -> controller.approveMember(group.getName(), user.GetName())));
                Button reject = new Button("Recusar");
                reject.setDisable(true);
                reject.setText("Recusar (TODO backend)");
                rows.getChildren().add(new HBox(8, new Label(user.GetName()), approve, reject));
            }
        }
        return new VBox(10, new Label("Solicitacoes pendentes - " + group.getName()), rows);
    }

    private Parent createGroupPanel(GroupsController groupsController, UsersController usersController, Parent chatRoot, BorderPane center) {
        TextField name = new TextField();
        name.setPromptText("Nome do grupo");
        TextField search = new TextField();
        search.setPromptText("Pesquisar usuarios (visualizacao)");
        ListView<User> users = new ListView<>(usersController.users());
        users.setCellFactory(view -> ViewSupport.userCell(user -> usersController.isOnline(user.GetName())));
        Button create = new Button("Criar grupo");
        create.setOnAction(event -> run(() -> {
            groupsController.createGroup(name.getText());
            ViewSupport.showInfo("Grupo criado. Adicionar membros diretamente exige novo metodo no backend RMI.");
            refreshAll(groupsController, usersController);
            center.setCenter(chatRoot);
        }));
        return new VBox(10, new Label("Criar grupo"), name, search, users, create);
    }

    private void refreshAll(GroupsController groupsController, UsersController usersController) throws RemoteException {
        groupsController.refreshGroups();
        usersController.refreshUsers();
        rebuildConversations(groupsController, usersController);
    }

    private void rebuildConversations(GroupsController groupsController, UsersController usersController) {
        conversations.clear();
        String current = rmiClientService.getCurrentUser();
        Set<String> online = new HashSet<>();
        usersController.onlineUsers().forEach(user -> online.add(user.GetName()));
        groupsController.groups().forEach(group -> conversations.add(ConversationItem.group(group)));
        usersController.users().stream()
                .filter(user -> !user.GetName().equals(current))
                .forEach(user -> conversations.add(ConversationItem.privateUser(user, online.contains(user.GetName()))));
    }

    private static boolean isMember(Group group, String userName) {
        return group.getMembers().stream().anyMatch(user -> user.GetName().equals(userName));
    }

    private static boolean isPending(Group group, String userName) {
        return group.getPendingMembers().stream().anyMatch(user -> user.GetName().equals(userName));
    }

    private static Scene styledScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(MainApp.class.getResource("/Styles/whatsut.css").toExternalForm());
        return scene;
    }

    private static void configureCallbacks(UiCallbackHandler callbackHandler, GroupsController groupsController,
            ChatController chatController, UsersController usersController, RemoteUiAction refreshAction) {
        callbackHandler.onGroupsRefresh(() -> run(refreshAction));
        callbackHandler.onJoinRequest((groupName, requesterName) -> run(refreshAction));
        callbackHandler.onGroupMessage(chatController::appendGroupMessage);
        callbackHandler.onPrivateMessage(chatController::appendPrivateMessage);
        callbackHandler.onError(ViewSupport::showError);
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
