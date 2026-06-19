package com.example.Socket;

import com.example.Models.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WhatsUTSocket implements AutoCloseable {
    private final int port;
    private final Map<String, ClientConnection> onlineUsers = new ConcurrentHashMap<>();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public WhatsUTSocket(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        Thread serverThread = new Thread(this::acceptConnections, "whatsut-notification-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void notifyUser(User user, String event) {
        ClientConnection connection = onlineUsers.get(user.GetName());
        if (connection != null && !connection.send(event)) {
            disconnect(user.GetName(), connection);
        }
    }

    public void notifyUsers(Collection<User> users, String event) {
        for (User user : users) {
            notifyUser(user, event);
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread clientThread = new Thread(() -> handleClient(socket), "whatsut-notification-client");
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (IOException exception) {
                if (running) {
                    System.err.println("Erro ao aceitar conexao de notificacao: " + exception.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        ClientConnection connection = null;
        String username = null;
        try {
            connection = new ClientConnection(socket);
            username = connection.readUsername();
            if (username == null || username.isBlank()) {
                connection.close();
                return;
            }

            ClientConnection previous = onlineUsers.put(username, connection);
            if (previous != null) {
                previous.close();
            }

            connection.waitUntilDisconnected();
        } catch (IOException exception) {
            // A desconexao tambem chega aqui quando o cliente fecha o socket.
        } finally {
            if (username != null && connection != null) {
                disconnect(username, connection);
            } else if (connection != null) {
                connection.close();
            }
        }
    }

    private void disconnect(String username, ClientConnection connection) {
        onlineUsers.remove(username, connection);
        connection.close();
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
        onlineUsers.values().forEach(ClientConnection::close);
        onlineUsers.clear();
    }

    private static class ClientConnection {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        String readUsername() throws IOException {
            return reader.readLine();
        }

        boolean send(String event) {
            writer.println(event);
            return !writer.checkError();
        }

        void waitUntilDisconnected() throws IOException {
            while (reader.readLine() != null) {
                // O canal TCP e unidirecional para eventos apos a identificacao inicial.
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Socket ja encerrado.
            }
        }
    }}