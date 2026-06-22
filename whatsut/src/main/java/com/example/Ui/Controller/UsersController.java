package com.example.Ui.Controller;

import com.example.Models.User;
import com.example.Ui.Service.RmiClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.rmi.RemoteException;

public class UsersController {
    private final RmiClientService rmiClientService;
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<User> onlineUsers = FXCollections.observableArrayList();

    public UsersController(RmiClientService rmiClientService) {
        this.rmiClientService = rmiClientService;
    }

    public ObservableList<User> users() {
        return users;
    }

    public ObservableList<User> onlineUsers() {
        return onlineUsers;
    }

    public void refreshOnlineUsers() throws RemoteException {
        onlineUsers.setAll(rmiClientService.listOnlineUsers());
    }

    public void refreshUsers() throws RemoteException {
        users.setAll(rmiClientService.listUsers());
        refreshOnlineUsers();
    }

    public boolean isOnline(String userName) {
        return onlineUsers.stream().anyMatch(user -> user.GetName().equals(userName));
    }
}