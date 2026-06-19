package com.example;

import com.example.Rmi.ServerRemote;
import com.example.Service.ServerService;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicReference;

public class Server {
    private static final int RMI_PORT = 1099;
    private static final String SERVICE_NAME = "WhatsUT";

    public static void main(String[] args) throws RemoteException, AlreadyBoundException, IOException {
        int rmiPort = RMI_PORT;
        String serviceName = SERVICE_NAME;

        System.setProperty("java.rmi.server.hostname", getHostname());

        AtomicReference<ServerService> serviceReference = new AtomicReference<>();
        ServerService service = new ServerService();
        serviceReference.set(service);

        Registry registry = LocateRegistry.createRegistry(rmiPort);
        ServerRemote remoteService = service;
        registry.bind(serviceName, remoteService);

        System.out.printf("Servidor RMI WhatsUT iniciado em rmi://%s:%d/%s%n", getHostname(), rmiPort, serviceName);
    }

    private static String getHostname() {
        String value = System.getenv("RMI_HOSTNAME");
        if (value == null || value.isBlank()) {
            return "localhost";
        }
        return value;
    }
}