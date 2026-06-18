package com.example;

import com.example.Rmi.WhatsUTRemote;
import com.example.Service.WhatsUTService;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String DEFAULT_SERVICE_NAME = "WhatsUT";

    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        int port = getPort();
        String serviceName = getServiceName();

        System.setProperty("java.rmi.server.hostname", getHostname());

        Registry registry = LocateRegistry.createRegistry(port);
        WhatsUTRemote service = new WhatsUTService();
        registry.bind(serviceName, service);

        System.out.printf("Servidor RMI WhatsUT iniciado em rmi://%s:%d/%s%n", getHostname(), port, serviceName);
    }

    private static int getPort() {
        String value = System.getenv("RMI_PORT");
        if (value == null || value.isBlank()) {
            return DEFAULT_RMI_PORT;
        }
        return Integer.parseInt(value);
    }

    private static String getServiceName() {
        String value = System.getenv("RMI_SERVICE_NAME");
        if (value == null || value.isBlank()) {
            return DEFAULT_SERVICE_NAME;
        }
        return value;
    }

    private static String getHostname() {
        String value = System.getenv("RMI_HOSTNAME");
        if (value == null || value.isBlank()) {
            return "localhost";
        }
        return value;
    }
}