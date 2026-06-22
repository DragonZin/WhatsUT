package com.example.Rmi;

import com.example.Models.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRemote extends Remote {
    void refreshGroups() throws RemoteException;

    default void onGroupCreated(String groupName) throws RemoteException { refreshGroups(); }
    default void onGroupJoinApproved(String groupName) throws RemoteException { refreshGroups(); }
    default void onParticipantAdded(String groupName, String userName) throws RemoteException { refreshGroups(); }
    default void onParticipantRemoved(String groupName, String userName) throws RemoteException { refreshGroups(); }

    void refreshRequest(String groupName, String userName) throws RemoteException;

    void refreshMessage(String groupName, Message message) throws RemoteException;

    void refreshPrivateMessage(String senderName, Message message) throws RemoteException;
}