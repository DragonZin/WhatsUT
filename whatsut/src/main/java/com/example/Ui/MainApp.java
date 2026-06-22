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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Alert;
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
        ChatView chatView = new ChatView(chatController,
                group -> showPendingRequestsDialog(stage, groupsController, usersController, group.getName()),
                group -> showGroupMembersDialog(stage, groupsController, usersController, chatController, group));
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
        list.setCellFactory(view -> ViewSupport.conversationCell(rmiClientService.getCurrentUser()));
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


    private void showPendingRequestsDialog(Stage owner, GroupsController groupsController, UsersController usersController, String groupName) {
        Group selectedGroup = groupsController.groups().stream()
                .filter(group -> group.getName().equals(groupName))
                .findFirst()
                .orElse(null);
        if (selectedGroup == null || !selectedGroup.getAdmin().GetName().equals(rmiClientService.getCurrentUser())) {
            ViewSupport.showError(new IllegalAccessException("Apenas o administrador pode gerenciar solicitacoes."));
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Solicitacoes pendentes");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(MainApp.class.getResource("/Styles/whatsut.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("pending-requests-dialog");

        Label title = new Label();
        title.getStyleClass().add("pending-requests-title");
        Label subtitle = new Label("Revise quem pode entrar no grupo.");
        subtitle.getStyleClass().add("muted-label");
        VBox rows = new VBox(8);
        rows.getStyleClass().add("pending-request-list");
        VBox content = new VBox(8, title, subtitle, rows);
        content.getStyleClass().add("pending-requests-content");
        dialog.getDialogPane().setContent(content);

        Runnable[] refreshDialog = new Runnable[1];
        refreshDialog[0] = () -> {
            Group group = groupsController.groups().stream()
                    .filter(item -> item.getName().equals(groupName))
                    .findFirst()
                    .orElse(null);
            if (group == null) {
                dialog.close();
                return;
            }

            title.setText("Solicitacoes pendentes (" + group.getPendingMembers().size() + ")");
            rows.getChildren().clear();
            if (group.getPendingMembers().isEmpty()) {
                Label empty = new Label("Nenhuma solicitacao pendente.");
                empty.getStyleClass().add("muted-label");
                rows.getChildren().add(empty);
                return;
            }

            for (User user : group.getPendingMembers()) {
                Label name = new Label(user.GetName());
                name.getStyleClass().add("pending-request-name");
                Button approve = new Button("Aprovar");
                Button reject = new Button("Recusar");
                reject.getStyleClass().add("secondary-button");
                approve.setOnAction(event -> resolveJoinRequest(groupsController, usersController, groupName, user.GetName(), true, refreshDialog[0]));
                reject.setOnAction(event -> resolveJoinRequest(groupsController, usersController, groupName, user.GetName(), false, refreshDialog[0]));
                HBox row = new HBox(8, name, approve, reject);
                row.getStyleClass().add("pending-request-row");
                HBox.setHgrow(name, Priority.ALWAYS);
                rows.getChildren().add(row);
            }
        };
        refreshDialog[0].run();
        dialog.show();
    }

    private void showGroupMembersDialog(Stage owner, GroupsController groupsController, UsersController usersController,
            ChatController chatController, Group group) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Membros do grupo");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(MainApp.class.getResource("/Styles/whatsut.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("group-members-dialog");

        Label title = new Label(group.getName());
        title.getStyleClass().add("group-members-title");
        Label subtitle = new Label();
        subtitle.getStyleClass().add("muted-label");
        VBox rows = new VBox(8);
        rows.getStyleClass().add("group-members-list");
        Button refresh = new Button("Atualizar");
        refresh.getStyleClass().add("secondary-button");
        boolean isAdmin = group.getAdmin().GetName().equals(rmiClientService.getCurrentUser());
        Button leaveGroup = new Button("Sair e encerrar grupo");
        leaveGroup.getStyleClass().add("danger-button");
        leaveGroup.setVisible(isAdmin);
        leaveGroup.setManaged(isAdmin);
        VBox content = new VBox(8, title, subtitle, rows, refresh, leaveGroup);
        content.getStyleClass().add("group-members-content");
        dialog.getDialogPane().setContent(content);

        Runnable[] reload = new Runnable[1];
        reload[0] = () -> {
            try {
                java.util.List<User> members = chatController.listSelectedGroupMembers();
                subtitle.setText(members.size() + (members.size() == 1 ? " membro" : " membros"));
                rows.getChildren().clear();
                for (User member : members) {
                    Label name = new Label(member.GetName());
                    name.getStyleClass().add("group-member-name");
                    Label role = new Label(member.GetName().equals(group.getAdmin().GetName()) ? "Administrador" : "Membro");
                    role.getStyleClass().add(member.GetName().equals(group.getAdmin().GetName())
                            ? "group-member-admin" : "group-member-role");
                    VBox details = new VBox(2, name, role);
                    HBox row = new HBox(details);
                    row.getStyleClass().add("group-member-row");
                    HBox.setHgrow(details, Priority.ALWAYS);
                    if (isAdmin && !member.GetName().equals(group.getAdmin().GetName())) {
                        Button remove = new Button("Remover");
                        remove.getStyleClass().add("danger-button");
                        remove.setOnAction(event -> removeGroupMember(groupsController, usersController, chatController,
                                member.GetName(), reload[0]));
                        row.getChildren().add(remove);
                    }
                    rows.getChildren().add(row);
                }
            } catch (Exception exception) {
                ViewSupport.showError(exception);
                dialog.close();
            }
        };
        refresh.setOnAction(event -> reload[0].run());
        leaveGroup.setOnAction(event -> leaveGroupAsAdmin(dialog, groupsController, usersController, chatController, group.getName()));
        reload[0].run();
        dialog.show();
    }

    private void removeGroupMember(GroupsController groupsController, UsersController usersController,
            ChatController chatController, String userName, Runnable reload) {
        try {
            chatController.removeSelectedGroupMember(userName);
            refreshAll(groupsController, usersController);
            reload.run();
        } catch (Exception exception) {
            ViewSupport.showError(exception);
        }
    }

    private void leaveGroupAsAdmin(Dialog<?> dialog, GroupsController groupsController, UsersController usersController,
            ChatController chatController, String groupName) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Ao sair, o grupo \"" + groupName + "\" sera encerrado para todos os membros.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("Sair do grupo");
        confirmation.setHeaderText("Deseja sair e encerrar o grupo?");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            chatController.leaveSelectedGroupAsAdmin();
            refreshAll(groupsController, usersController);
            dialog.close();
        } catch (Exception exception) {
            ViewSupport.showError(exception);
        }
    }

    private void resolveJoinRequest(GroupsController groupsController, UsersController usersController, String groupName,
            String userName, boolean approve, Runnable refreshDialog) {
        try {
            if (approve) {
                groupsController.approveMember(groupName, userName);
            } else {
                groupsController.rejectMember(groupName, userName);
            }
            refreshAll(groupsController, usersController);
            refreshDialog.run();
        } catch (Exception exception) {
            ViewSupport.showError(exception);
        }
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
            if (!message.getSender().GetName().equals(rmiClientService.getCurrentUser())) {
                markUnread("P:" + senderName);
            }
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
