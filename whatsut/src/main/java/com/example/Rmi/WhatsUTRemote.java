package com.example.Rmi;

import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WhatsUTRemote extends Remote {
    User registerUser(String name, String password) throws RemoteException;

    boolean authenticate(String name, String password) throws RemoteException;

    void logout(String name) throws RemoteException;

    /*Group createGroup(String groupName, String adminName) throws RemoteException;

    boolean requestJoinGroup(String groupName, String userName) throws RemoteException;

    boolean approvePendingMember(String groupName, String adminName, String userName) throws RemoteException;

    boolean sendTextMessage(String groupName, String senderName, String content) throws RemoteException;

    List<Message> getMessages(String groupName, String userName) throws RemoteException;
*/
    List<Group> listGroups() throws RemoteException;

    List<User> listUsers() throws RemoteException;

    List<User> listAuthenticatedUsers() throws RemoteException;
}