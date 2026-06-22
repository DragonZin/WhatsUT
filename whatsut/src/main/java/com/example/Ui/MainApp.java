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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MainApp extends Application {
    private RmiClientService rmiClientService;
    private final ObservableList<ConversationItem> conversations = FXCollections.observableArrayList();
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    private ChatController activeChatController;
    private GroupsController activeGroupsController;
    private UsersController activeUsersController;

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
        activeGroupsController = groupsController;
        activeChatController = chatController;
        activeUsersController = usersController;
        ChatView chatView = new ChatView(chatController);
        BorderPane center = new BorderPane(chatView.root());
        Parent sidebar = conversationsSidebar(stage, callbackHandler, groupsController, usersController, chatController, center, chatView.root());
        configureCallbacks(callbackHandler, groupsController, chatController, usersController, () -> refreshAll(groupsController, usersController));
        run(() -> refreshAll(groupsController, usersController));
        SplitPane splitPane = new SplitPane(sidebar, center);
        splitPane.setDividerPositions(0.36);
        stage.setScene(styledScene(splitPane, 1040, 680));
    }

    private Parent conversationsSidebar(Stage stage, UiCallbackHandler callbackHandler, GroupsController groupsController, UsersController usersController,
            ChatController chatController, BorderPane center, Parent chatRoot) {
        ListView<ConversationItem> list = new ListView<>(conversations);
        list.setCellFactory(view -> ViewSupport.conversationCell());
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item == null) {
                return;
            }
            center.setCenter(chatRoot);
            center.setBottom(null);
            if (item.type() == ConversationItem.Type.PRIVATE) {
                unreadCounts.remove(item.key());
                rebuildConversations(groupsController, usersController);
                run(() -> chatController.selectPrivateUser(item.user().GetName()));
            } else if (isMember(item.group(), rmiClientService.getCurrentUser())) {
                unreadCounts.remove(item.key());
                rebuildConversations(groupsController, usersController);
                run(() -> chatController.selectGroup(item.group()));
                center.setBottom(pendingRequestsPanel(groupsController, item.group()));
            } else {
                chatController.showPlaceholder(item.group().getName(), "Voce ainda nao participa deste grupo");
                center.setCenter(joinRequestPanel(groupsController, item.group()));
            }
        });
        Button createGroup = new Button("Criar grupo");
        createGroup.setMaxWidth(Double.MAX_VALUE);
        createGroup.setOnAction(event -> center.setCenter(createGroupPanel(groupsController, usersController, chatRoot, center)));
        Button logout = new Button("Logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.getStyleClass().add("secondary-button");
        logout.setOnAction(event -> run(() -> logout(stage, callbackHandler)));
        Button close = new Button("Fechar aplicativo");
        close.setMaxWidth(Double.MAX_VALUE);
        close.getStyleClass().add("secondary-button");
        close.setOnAction(event -> stage.close());
        Button refresh = new Button("Atualizar");
        refresh.setMaxWidth(Double.MAX_VALUE);
        refresh.setOnAction(event -> run(() -> refreshAll(groupsController, usersController)));
        VBox sidebar = new VBox(8, new Label("Conversas"), createGroup, refresh, logout, close, list);
        sidebar.getStyleClass().add("sidebar");
        VBox.setVgrow(list, Priority.ALWAYS);
        return sidebar;
    }

    private Parent joinRequestPanel(GroupsController controller, Group group) {
        boolean pending = isPending(group, rmiClientService.getCurrentUser());
        Label title = new Label(group.getName());
        Label description = new Label(group.getDescription().isBlank() ? "Sem descricao" : group.getDescription());
        Label members = new Label(group.getMembers().size() + " membros");
        Label status = new Label(pending ? "Status: aguardando aprovacao" : "Status: nao solicitado");
        Button join = new Button(pending ? "Aguardando aprovacao" : "Solicitar entrada");
        join.setDisable(pending);
        join.setOnAction(event -> run(() -> { controller.joinGroup(group); status.setText("Status: aguardando aprovacao"); join.setDisable(true); }));
        Button cancel = new Button("Cancelar solicitacao");
        cancel.getStyleClass().add("secondary-button");
        cancel.setDisable(!pending);
        cancel.setOnAction(event -> run(() -> { controller.cancelJoinRequest(group); status.setText("Status: nao solicitado"); join.setDisable(false); cancel.setDisable(true); }));
        VBox panel = new VBox(10, title, description, members, status, new HBox(8, join, cancel));
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
                reject.getStyleClass().add("secondary-button");
                reject.setOnAction(event -> run(() -> controller.rejectMember(group.getName(), user.GetName())));
                rows.getChildren().add(new HBox(8, new Label(user.GetName()), approve, reject));
            }
        }
        VBox box = new VBox(10, new Label("Solicitacoes pendentes (" + group.getPendingMembers().size() + ") - " + group.getName()), rows);
        box.getStyleClass().add("panel-card");
        return box;
    }

    private Parent createGroupPanel(GroupsController groupsController, UsersController usersController, Parent chatRoot, BorderPane center) {
        TextField name = new TextField();
        name.setPromptText("Nome do grupo");
        TextArea description = new TextArea();
        description.setPromptText("Descricao do grupo");
        description.setPrefRowCount(3);
        TextField search = new TextField();
        search.setPromptText("Buscar usuarios");
        VBox userChecks = new VBox(6);
        Runnable rebuild = () -> {
            userChecks.getChildren().clear();
            String q = search.getText() == null ? "" : search.getText().toLowerCase();
            usersController.users().stream()
                    .filter(user -> !user.GetName().equals(rmiClientService.getCurrentUser()))
                    .filter(user -> user.GetName().toLowerCase().contains(q))
                    .forEach(user -> userChecks.getChildren().add(new CheckBox(user.GetName())));
        };
        search.textProperty().addListener((obs, old, value) -> rebuild.run());
        rebuild.run();
        Button create = new Button("Criar grupo");
        create.setOnAction(event -> run(() -> {
            java.util.List<String> selected = userChecks.getChildren().stream()
                    .filter(node -> node instanceof CheckBox checkBox && checkBox.isSelected())
                    .map(node -> ((CheckBox) node).getText())
                    .collect(Collectors.toList());
            groupsController.createGroup(name.getText(), description.getText(), selected);
            ViewSupport.showInfo("Grupo criado com " + selected.size() + " participante(s).");
            refreshAll(groupsController, usersController);
            center.setCenter(chatRoot);
        }));
        VBox panel = new VBox(10, new Label("Criar grupo"), name, description, search, new Label("Participantes"), userChecks, create);
        panel.getStyleClass().add("panel-card");
        return panel;
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
        groupsController.groups().forEach(group -> conversations.add(ConversationItem.group(group, unreadCounts.getOrDefault("G:" + group.getName(), 0))));
        usersController.users().stream()
                .filter(user -> !user.GetName().equals(current))
                .forEach(user -> conversations.add(ConversationItem.privateUser(user, online.contains(user.GetName()), unreadCounts.getOrDefault("P:" + user.GetName(), 0))));
    }


    private void logout(Stage stage, UiCallbackHandler callbackHandler) throws Exception {
        unreadCounts.clear();
        callbackHandler.clear();
        rmiClientService.close();
        LoginController loginController = new LoginController(rmiClientService);
        loginController.onLoginSuccess(userName -> showMainScene(stage, callbackHandler));
        stage.setScene(styledScene(new LoginView(loginController).root(), 480, 420));
    }


    private void markUnread(String key) {
        if (activeChatController == null || !activeChatController.currentConversationKey().equals(key)) {
            unreadCounts.merge(key, 1, Integer::sum);
        }
        if (activeGroupsController != null && activeUsersController != null) {
            rebuildConversations(activeGroupsController, activeUsersController);
        }
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

    private void configureCallbacks(UiCallbackHandler callbackHandler, GroupsController groupsController,
            ChatController chatController, UsersController usersController, RemoteUiAction refreshAction) {
        callbackHandler.onGroupsRefresh(() -> run(refreshAction));
        callbackHandler.onJoinRequest((groupName, requesterName) -> run(refreshAction));
        callbackHandler.onGroupMessage((groupName, message) -> {
            chatController.appendGroupMessage(groupName, message);
            markUnread("G:" + groupName);
        });
        callbackHandler.onPrivateMessage((senderName, message) -> {
            chatController.appendPrivateMessage(senderName, message);
            markUnread("P:" + senderName);
        });
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