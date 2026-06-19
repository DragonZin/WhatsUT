package com.example.Rmi;

import com.example.Models.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRemote extends Remote {
    void refreshGroups() throws RemoteException;

    void refreshRequest(String groupName, String userName) throws RemoteException;

    void refreshMessage(String groupName, Message message) throws RemoteException;

    void refreshPrivateMessage(String senderName, Message message) throws RemoteException;
}