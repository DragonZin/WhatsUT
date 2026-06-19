import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.User;
import com.example.Rmi.ServerRemote;
import com.example.Service.ClientService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Client {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String DEFAULT_SERVICE_NAME = "WhatsUT";
    private static final int AUTO_REPEAT_COUNT = 5;
    private static final String AUTO_PASSWORD = "123456";
    private static final DateTimeFormatter MESSAGE_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Scanner scanner = new Scanner(System.in);
    private final ServerRemote remote;
    private String currentUser;
    private ClientService clientService;

    public Client(ServerRemote remote) {
        this.remote = remote;
    }

    public static void main(String[] args) {
        String host = getArgument(args, 0, getEnvironment("WHATSUT_HOST", DEFAULT_HOST));
        int rmiPort = parsePort(getArgument(args, 1, getEnvironment("WHATSUT_RMI_PORT", String.valueOf(DEFAULT_RMI_PORT))), DEFAULT_RMI_PORT);
        String serviceName = getArgument(args, 2, getEnvironment("WHATSUT_SERVICE_NAME", DEFAULT_SERVICE_NAME));
        String rmiUrl = String.format("rmi://%s:%d/%s", host, rmiPort, serviceName);

        try {
            ServerRemote remote = (ServerRemote) Naming.lookup(rmiUrl);
            new Client(remote).run();
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
            Thread.currentThread().interrupt();
        }
    }

    private boolean showAnonymousMenu() throws RemoteException {
        System.out.println();
        System.out.println("1) Registar usuario");
        System.out.println("2) Login");
        System.out.println("3) Teste automatico (roda comandos 5 vezes)");
        System.out.println("0) Sair");
        String option = readRequired("Opcao: ");

        switch (option) {
            case "1" -> registerUser();
            case "2" -> login();
            case "3" -> runAutomaticTest();
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
        System.out.println("5) Enviar mensagem de texto para grupo");
        System.out.println("6) Ver mensagens do grupo");
        System.out.println("7) Listar membros de grupo");
        System.out.println("8) Listar usuarios autenticados");
        System.out.println("9) Enviar mensagem privada de texto");
        System.out.println("10) Ver mensagens privadas");
        System.out.println("11) Enviar arquivo para grupo");
        System.out.println("12) Enviar arquivo privado");
        System.out.println("13) Remover membro do grupo");
        System.out.println("14) Teste automatico (roda comandos 5 vezes)");
        System.out.println("15) Logout");
        System.out.println("0) Sair");
        String option = readRequired("Opcao: ");

        switch (option) {
            case "1" -> createGroup();
            case "2" -> listGroups();
            case "3" -> requestJoinGroup();
            case "4" -> approvePendingMember();
            case "5" -> sendGroupTextMessage();
            case "6" -> showGroupMessages();
            case "7" -> listGroupUsers();
            case "8" -> listAuthenticatedUsers();
            case "9" -> sendPrivateTextMessage();
            case "10" -> showPrivateMessages();
            case "11" -> sendGroupFileMessage();
            case "12" -> sendPrivateFileMessage();
            case "13" -> deleteUserFromGroup();
            case "14" -> runAutomaticTest();
            case "15" -> logout();
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
        ClientService service = new ClientService(name, remote);
        service.onRefreshGroups(() -> System.out.printf("%n[Notificacao] Lista de grupos atualizada.%nOpcao: "));
        service.onRefreshRequest((groupName, requesterName) -> System.out.printf("%n[Notificacao] Novo pedido de %s no grupo %s.%nOpcao: ", requesterName, groupName));
        service.onRefreshMessage((groupName, message) -> System.out.printf("%n[Notificacao] Nova mensagem em %s de %s.%nOpcao: ", groupName, message.getSender().GetName()));
        service.onRefreshPrivateMessage((senderName, message) -> System.out.printf("%n[Notificacao] Nova mensagem privada de %s.%nOpcao: ", senderName));
        service.register(password);
        clientService = service;
        currentUser = name;
        System.out.println("Login efetuado com sucesso.");
    }

    private void createGroup() throws RemoteException {
        Group group = remote.createGroup(readRequired("Nome do grupo: "), currentUser);
        System.out.println("Grupo criado: " + group.getName());
    }

    private void listGroups() throws RemoteException {
        printGroups(remote.listGroups(currentUser));
    }

    private void requestJoinGroup() throws RemoteException {
        boolean requested = remote.requestJoinGroup(readRequired("Nome do grupo: "), currentUser);
        System.out.println(requested ? "Pedido enviado." : "Ja e membro ou ja tem pedido pendente.");
    }

    private void approvePendingMember() throws RemoteException {
        String groupName = readRequired("Nome do grupo: ");
        String userName = readRequired("Usuario a aprovar: ");
        boolean approved = remote.approvePendingMember(groupName, currentUser, userName);
        System.out.println(approved ? "Membro aprovado." : "Nao havia pedido pendente para esse usuario.");
    }

    private void sendGroupTextMessage() throws RemoteException {
        String groupName = readRequired("Nome do grupo: ");
        String content = readRequired("Mensagem: ");
        remote.sendGroupTextMessage(groupName, currentUser, new TextMessage(content, new User(currentUser, "")));
        System.out.println("Mensagem enviada.");
    }

    private void sendPrivateTextMessage() throws RemoteException {
        String receiver = readRequired("Usuario destino: ");
        String content = readRequired("Mensagem: ");
        remote.sendPrivateTextMessage(currentUser, receiver, new TextMessage(content, new User(currentUser, "")));
        System.out.println("Mensagem privada enviada.");
    }

    private void sendGroupFileMessage() throws RemoteException {
        String groupName = readRequired("Nome do grupo: ");
        FileMessage fileMessage = readFileMessage();
        remote.sendGroupFileMessage(groupName, currentUser, fileMessage);
        System.out.println("Arquivo enviado para o grupo.");
    }

    private void sendPrivateFileMessage() throws RemoteException {
        String receiver = readRequired("Usuario destino: ");
        FileMessage fileMessage = readFileMessage();
        remote.sendPrivateFileMessage(currentUser, receiver, fileMessage);
        System.out.println("Arquivo privado enviado.");
    }

    private void showGroupMessages() throws RemoteException {
        printMessages(remote.getMessages(readRequired("Nome do grupo: "), currentUser));
    }

    private void showPrivateMessages() throws RemoteException {
        printMessages(remote.getPrivateMessages(currentUser, readRequired("Usuario da conversa: ")));
    }

    private void listGroupUsers() throws RemoteException {
        List<User> users = remote.listGroupUsers(currentUser, readRequired("Nome do grupo: "));
        printUsers(users);
    }

    private void listAuthenticatedUsers() throws RemoteException {
        printUsers(remote.listAuthenticatedUsers(currentUser));
    }

    private void deleteUserFromGroup() throws RemoteException {
        String groupName = readRequired("Nome do grupo: ");
        String userName = readRequired("Usuario a remover: ");
        boolean removed = remote.deleteUserFromGroup(groupName, currentUser, userName);
        System.out.println(removed ? "Membro removido." : "Usuario nao era membro do grupo.");
    }

    private void runAutomaticTest() throws RemoteException {
        String prefix = "auto" + System.currentTimeMillis();
        System.out.printf("Iniciando teste automatico com prefixo %s (%d repeticoes por comando).%n", prefix, AUTO_REPEAT_COUNT);

        for (int i = 1; i <= AUTO_REPEAT_COUNT; i++) {
            final int n = i;
            safeRun("registar usuario admin " + n, () -> remote.registerUser(prefix + "_admin" + n, AUTO_PASSWORD));
            safeRun("registar usuario membro " + n, () -> remote.registerUser(prefix + "_member" + n, AUTO_PASSWORD));
        }

        for (int i = 1; i <= AUTO_REPEAT_COUNT; i++) {
            final int n = i;
            String admin = prefix + "_admin" + n;
            String member = prefix + "_member" + n;
            String group = prefix + "_group" + n;
            try (ClientService adminSession = loginForAutomaticTest(admin); ClientService memberSession = loginForAutomaticTest(member)) {
                safeRun("criar grupo " + n, () -> remote.createGroup(group, admin));
                safeRun("listar grupos " + n, () -> { printGroups(remote.listGroups(admin)); return null; });
                safeRun("pedir entrada " + n, () -> remote.requestJoinGroup(group, member));
                safeRun("aprovar membro " + n, () -> remote.approvePendingMember(group, admin, member));
                safeRun("listar membros " + n, () -> { printUsers(remote.listGroupUsers(admin, group)); return null; });
                safeRun("mensagem grupo texto " + n, () -> remote.sendGroupTextMessage(group, admin, new TextMessage("Mensagem automatica " + n, new User(admin, ""))));
                safeRun("mensagem privada texto " + n, () -> remote.sendPrivateTextMessage(admin, member, new TextMessage("Privada automatica " + n, new User(admin, ""))));
                safeRun("arquivo grupo " + n, () -> remote.sendGroupFileMessage(group, admin, new FileMessage("grupo-auto-" + n + ".txt", toByteObjects(("arquivo grupo " + n).getBytes()), new User(admin, ""))));
                safeRun("arquivo privado " + n, () -> remote.sendPrivateFileMessage(admin, member, new FileMessage("privado-auto-" + n + ".txt", toByteObjects(("arquivo privado " + n).getBytes()), new User(admin, ""))));
                safeRun("ver mensagens grupo " + n, () -> { printMessages(remote.getMessages(group, admin)); return null; });
                safeRun("ver mensagens privadas " + n, () -> { printMessages(remote.getPrivateMessages(admin, member)); return null; });
                safeRun("listar autenticados " + n, () -> { printUsers(remote.listAuthenticatedUsers(admin)); return null; });
                safeRun("remover membro " + n, () -> remote.deleteUserFromGroup(group, admin, member));
            } catch (Exception exception) {
                System.out.println("[ERRO] ciclo automatico " + i + ": " + exception.getMessage());
            }
        }
        System.out.println("Teste automatico terminado.");
    }

    private ClientService loginForAutomaticTest(String userName) throws RemoteException {
        ClientService service = new ClientService(userName, remote);
        service.register(AUTO_PASSWORD);
        return service;
    }

    private void safeRun(String description, RemoteAction action) {
        try {
            Object result = action.run();
            System.out.printf("[OK] %s%s%n", description, result == null ? "" : ": " + result);
        } catch (Exception exception) {
            System.out.printf("[ERRO] %s: %s%n", description, exception.getMessage());
        }
    }

    private FileMessage readFileMessage() {
        String pathValue = readRequired("Caminho do arquivo: ");
        try {
            Path path = Path.of(pathValue);
            return new FileMessage(path.getFileName().toString(), toByteObjects(Files.readAllBytes(path)), new User(currentUser, ""));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Nao foi possivel ler o arquivo: " + exception.getMessage(), exception);
        }
    }

    private static Byte[] toByteObjects(byte[] bytes) {
        Byte[] objects = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            objects[i] = bytes[i];
        }
        return objects;
    }

    private void printGroups(List<Group> groups) {
        if (groups.isEmpty()) {
            System.out.println("Nao existem grupos.");
            return;
        }
        groups.forEach(group -> System.out.printf("- %s (admin: %s, membros: %d, pendentes: %d)%n",
                group.getName(), group.getAdmin().GetName(), group.getMembers().size(), group.getPendingMembers().size()));
    }

    private void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("Sem usuarios.");
            return;
        }
        users.forEach(user -> System.out.println("- " + user.GetName()));
    }

    private void printMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            System.out.println("Sem mensagens.");
            return;
        }
        for (Message message : messages) {
            String content = switch (message) {
                case TextMessage textMessage -> textMessage.getContent();
                case FileMessage fileMessage -> "[arquivo] " + fileMessage.getFileName() + " (" + fileMessage.getContent().length + " bytes)";
                default -> "[mensagem nao suportada]";
            };
            System.out.printf("[%s] %s: %s%n", MESSAGE_DATE_FORMATTER.format(message.getTimestamp()), message.getSender().GetName(), content);
        }
    }

    private void logout() throws RemoteException {
        closeClientService();
        currentUser = null;
        System.out.println("Logout efetuado.");
    }

    private void shutdown() {
        closeClientService();
    }

    private void closeClientService() {
        if (clientService != null) {
            try {
                clientService.close();
            } catch (RemoteException ignored) {
                // O servidor pode ja estar indisponivel ao encerrar o cliente.
            }
            clientService = null;
        }
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

    @FunctionalInterface
    private interface RemoteAction {
        Object run() throws Exception;
    }
}