package com.example.Ui.Controller;

import com.example.Ui.Service.RmiClientService;

import java.rmi.RemoteException;
import java.util.Objects;
import java.util.function.Consumer;

public class LoginController {
    private final RmiClientService rmiClientService;
    private Consumer<String> loginSuccessHandler = userName -> { };

    public LoginController(RmiClientService rmiClientService) {
        this.rmiClientService = Objects.requireNonNull(rmiClientService);
    }

    public void onLoginSuccess(Consumer<String> handler) {
        loginSuccessHandler = handler == null ? userName -> { } : handler;
    }

    public void register(String userName, String password) throws RemoteException {
        validateCredentials(userName, password);
        rmiClientService.registerUser(userName.trim(), password);
    }

    public void login(String userName, String password) throws RemoteException {
        validateCredentials(userName, password);
        rmiClientService.login(userName.trim(), password);
        loginSuccessHandler.accept(userName.trim());
    }

    private static void validateCredentials(String userName, String password) {
        if (userName == null || userName.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Informe usuario e password.");
        }
    }
}