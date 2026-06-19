package com.example;

import com.example.Socket.WhatsUTSocket;
import com.example.Rmi.WhatsUTRemote;
import com.example.Service.WhatsUTService;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicReference;

public class Server {
    private static final int RMI_PORT = 1099;
    private static final int NOTIFICATION_PORT = 5000;
    private static final String SERVICE_NAME = "WhatsUT";

    public static void main(String[] args) throws RemoteException, AlreadyBoundException, IOException {
        int rmiPort = RMI_PORT;
        int notificationPort = NOTIFICATION_PORT;
        String serviceName = SERVICE_NAME;

        System.setProperty("java.rmi.server.hostname", getHostname());

        AtomicReference<WhatsUTService> serviceReference = new AtomicReference<>();
        WhatsUTSocket notificationServer = new WhatsUTSocket(notificationPort);
        WhatsUTService service = new WhatsUTService(notificationServer);
        serviceReference.set(service);

        notificationServer.start();

        Registry registry = LocateRegistry.createRegistry(rmiPort);
        WhatsUTRemote remoteService = service;
        registry.bind(serviceName, remoteService);

        System.out.printf("Servidor RMI WhatsUT iniciado em rmi://%s:%d/%s%n", getHostname(), rmiPort, serviceName);
        System.out.printf("Servidor TCP de notificacoes iniciado em %s:%d%n", getHostname(), notificationPort);
    }

    private static String getHostname() {
        String value = System.getenv("RMI_HOSTNAME");
        if (value == null || value.isBlank()) {
            return "localhost";
        }
        return value;
    }
}