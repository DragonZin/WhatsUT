import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.User;
import com.example.Rmi.ServerRemote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Client {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final int DEFAULT_SOCKET_PORT = 5000;
    private static final String DEFAULT_SERVICE_NAME = "WhatsUT";
    private static final DateTimeFormatter MESSAGE_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Scanner scanner = new Scanner(System.in);
    private final ServerRemote remote;
    private final String notificationHost;
    private final int notificationPort;
    private String currentUser;
    private NotificationClient notificationClient;

    public Client(ServerRemote remote, String notificationHost, int notificationPort) {
        this.remote = remote;
        this.notificationHost = notificationHost;
        this.notificationPort = notificationPort;
    }

    public static void main(String[] args) {
        String host = getArgument(args, 0, getEnvironment("WHATSUT_HOST", DEFAULT_HOST));
        int rmiPort = parsePort(getArgument(args, 1, getEnvironment("WHATSUT_RMI_PORT", String.valueOf(DEFAULT_RMI_PORT))), DEFAULT_RMI_PORT);
        int socketPort = parsePort(getArgument(args, 2, getEnvironment("WHATSUT_SOCKET_PORT", String.valueOf(DEFAULT_SOCKET_PORT))), DEFAULT_SOCKET_PORT);
        String serviceName = getArgument(args, 3, getEnvironment("WHATSUT_SERVICE_NAME", DEFAULT_SERVICE_NAME));
        String rmiUrl = String.format("rmi://%s:%d/%s", host, rmiPort, serviceName);

        try {
            ServerRemote remote = (ServerRemote) Naming.lookup(rmiUrl);
            new Client(remote, host, socketPort).run();
        } catch (Exception exception) {
            System.err.println("Nao foi possivel iniciar o cliente: " + exception.getMessage());
        }
    }

    private void run() {
        consoleClear();
        System.out.println("=== WhatsUT Console Client ===");
        boolean running = true;
        while (running) {
            try {
                if (currentUser == null) {
                    running = showAnonymousMenu();
                } else {
                    running = showAuthenticatedMenu();
                }
            } catch (RemoteException exception) {
                System.out.println("Erro remoto: " + exception.getMessage());
            } catch (RuntimeException exception) {
                System.out.println("Erro: " + exception.getMessage());
            }
        }
        shutdown();
        System.out.println("Ate logo!");
    }

    private void consoleClear() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean showAnonymousMenu() throws RemoteException {
        System.out.println();
        System.out.println("1) Registar usuario");
        System.out.println("2) Login");
        System.out.println("0) Sair");
        String option = readRequired("Opcao: ");

        switch (option) {
            case "1" -> registerUser();
            case "2" -> login();
            case "0" -> { return false; }
            default -> System.out.println("Opcao invalida.");
        }
        return true;
    }

    private boolean showAuthenticatedMenu() throws RemoteException {
        System.out.println();
        System.out.printf("Usuario: %s%n", currentUser);
        System.out.println("1) Criar grupo");
        System.out.println("2) Listar grupos");
        System.out.println("3) Pedir entrada em grupo");
        System.out.println("4) Aprovar membro pendente");
        System.out.println("5) Enviar mensagem");
        System.out.println("6) Ver mensagens");
        System.out.println("7) Listar membros de grupo");
        System.out.println("8) Listar Usuarios autenticados");
        System.out.println("9) Logout");
        System.out.println("0) Sair");
        String option = readRequired("Opcao: ");

        switch (option) {
            case "1" -> createGroup();
            case "2" -> listGroups();
            case "3" -> requestJoinGroup();
            case "4" -> approvePendingMember();
            case "5" -> sendMessage();
            case "6" -> showMessages();
            case "7" -> listGroupUsers();
            case "8" -> listAuthenticatedUsers();
            case "9" -> logout();
            case "0" -> { return false; }
            default -> System.out.println("Opcao invalida.");
        }
        return true;
    }

    private void registerUser() throws RemoteException {
        String name = readRequired("Nome: ");
        String password = readRequired("Password: ");
        User user = remote.registerUser(name, password);
        System.out.println("Usuario registado: " + user.GetName());
    }

    private void login() throws RemoteException {
        String name = readRequired("Nome: ");
        String password = readRequired("Password: ");
        if (remote.authenticate(name, password)) {
            currentUser = name;
            startNotifications(name);
            System.out.println("Login efetuado com sucesso.");
        } else {
            System.out.println("Credenciais invalidas.");
        }
    }

    private void createGroup() throws RemoteException {
        Group group = remote.createGroup(readRequired("Nome do grupo: "), currentUser);
        System.out.println("Grupo criado: " + group.getName());
    }

    private void listGroups() throws RemoteException {
        List<Group> groups = remote.listGroups(currentUser);
        if (groups.isEmpty()) {
            System.out.println("Nao existem grupos.");
            return;
        }
        groups.forEach(group -> System.out.printf("- %s (admin: %s, membros: %d, pendentes: %d)%n",
                group.getName(), group.getAdmin().GetName(), group.getMembers().size(), group.getPendingMembers().size()));
    }

    private void requestJoinGroup() throws RemoteException {
        boolean requested = remote.requestJoinGroup(readRequired("Nome do grupo: "), currentUser);
        System.out.println(requested ? "Pedido enviado." : "Ja e membro ou ja tem pedido pendente.");
    }

    private void approvePendingMember() throws RemoteException {
        String groupName = readRequired("Nome do grupo: ");
        String userName = readRequired("Usuario a aprovar: ");
        boolean approved = remote.approvePendingMember(groupName, currentUser, userName);
        System.out.println(approved ? "Membro aprovado." : "Nao havia pedido pendente para esse Usuario.");
    }

    private void sendMessage() throws RemoteException {
        String groupName = readRequired("Nome do grupo: ");
        String content = readRequired("Mensagem: ");
        remote.sendTextMessage(groupName, currentUser, content);
        System.out.println("Mensagem enviada.");
    }

    private void showMessages() throws RemoteException {
        List<Message> messages = remote.getMessages(readRequired("Nome do grupo: "), currentUser);
        if (messages.isEmpty()) {
            System.out.println("Sem mensagens.");
            return;
        }
        for (Message message : messages) {
            String content = message instanceof TextMessage textMessage ? textMessage.getContent() : "[mensagem nao suportada]";
            System.out.printf("[%s] %s: %s%n", MESSAGE_DATE_FORMATTER.format(message.getTimestamp()), message.getSender().GetName(), content);
        }
    }

    private void listGroupUsers() throws RemoteException {
        List<User> users = remote.listGroupUsers(currentUser, readRequired("Nome do grupo: "));
        users.forEach(user -> System.out.println("- " + user.GetName()));
    }

    private void listAuthenticatedUsers() throws RemoteException {
        List<User> users = remote.listAuthenticatedUsers(currentUser);
        users.forEach(user -> System.out.println("- " + user.GetName()));
    }

    private void logout() throws RemoteException {
        remote.logout(currentUser);
        stopNotifications();
        currentUser = null;
        System.out.println("Logout efetuado.");
    }

    private void startNotifications(String username) {
        stopNotifications();
        notificationClient = new NotificationClient(notificationHost, notificationPort, username);
        notificationClient.start();
    }

    private void stopNotifications() {
        if (notificationClient != null) {
            notificationClient.close();
            notificationClient = null;
        }
    }

    private void shutdown() {
        if (currentUser != null) {
            try {
                remote.logout(currentUser);
            } catch (RemoteException ignored) {
                // O servidor pode ja estar indisponivel ao encerrar o cliente.
            }
        }
        stopNotifications();
    }

    private String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("Valor obrigatorio.");
        }
    }

    private static String getArgument(String[] args, int index, String defaultValue) {
        return args.length > index && !args[index].isBlank() ? args[index] : defaultValue;
    }

    private static String getEnvironment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int parsePort(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static class NotificationClient implements AutoCloseable {
        private final String host;
        private final int port;
        private final String username;
        private volatile boolean running = true;
        private Socket socket;

        NotificationClient(String host, int port, String username) {
            this.host = host;
            this.port = port;
            this.username = username;
        }

        void start() {
            Thread thread = new Thread(this::listen, "whatsut-notifications");
            thread.setDaemon(true);
            thread.start();
        }

        private void listen() {
            try (Socket connectedSocket = new Socket(host, port);
                 PrintWriter writer = new PrintWriter(connectedSocket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(connectedSocket.getInputStream()))) {
                socket = connectedSocket;
                writer.println(username);
                String event;
                while (running && (event = reader.readLine()) != null) {
                    System.out.printf("%n[Notificacao] %s%nOpcao: ", describeEvent(event));
                }
            } catch (IOException exception) {
                if (running) {
                    System.out.printf("%n[Notificacao] Ligacao ao socket falhou: %s%n", exception.getMessage());
                }
            }
        }

        private String describeEvent(String event) {
            return switch (event) {
                case "REFRESH_GROUPS" -> "Novo grupo criado."; //Quando receber essa notificao faz um listGroups para atualizar a lista de grupos
                case "REFRESH_REQUEST" -> "Novo pedido de entrada em grupo."; //Quando receber essa notificao indica pro admin que tem um novo pedido
                case "REFRESH_MESSAGE" -> "Nova mensagem recebida.";//Quando receber essa notificao faz um showMessages para atualizar a lista de mensagens
                default -> event;
            };
        }

        @Override
        public void close() {
            running = false;
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Socket ja encerrado.
                }
            }
        }
    }
}