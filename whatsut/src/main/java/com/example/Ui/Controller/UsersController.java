package com.example.Ui.Controller;

import com.example.Models.User;
import com.example.Ui.Service.RmiClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.rmi.RemoteException;

public class UsersController {
    private final RmiClientService rmiClientService;
    private final ObservableList<User> onlineUsers = FXCollections.observableArrayList();

    public UsersController(RmiClientService rmiClientService) {
        this.rmiClientService = rmiClientService;
    }

    public ObservableList<User> onlineUsers() {
        return onlineUsers;
    }

    public void refreshOnlineUsers() throws RemoteException {
        onlineUsers.setAll(rmiClientService.listOnlineUsers());
    }
}
