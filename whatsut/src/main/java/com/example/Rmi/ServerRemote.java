package com.example.Rmi;

import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerRemote extends Remote {
    User registerUser(String name, String password) throws RemoteException;

    boolean authenticate(String name, String password) throws RemoteException;

    void logout(String name) throws RemoteException;

    Group createGroup(String groupName, String adminName) throws RemoteException;

    boolean requestJoinGroup(String groupName, String userName) throws RemoteException;

    boolean approvePendingMember(String groupName, String adminName, String userName) throws RemoteException;

    boolean deleteUserFromGroup(String groupName, String adminName, String userName) throws RemoteException;

    boolean sendTextMessage(String groupName, String senderName, String content) throws RemoteException;
    
    boolean sendFileMessage(String groupName, String senderName, FileMessage fileMessage) throws RemoteException;

    List<Message> getMessages(String groupName, String userName) throws RemoteException;

    List<Group> listGroups(String userName) throws RemoteException;
    
    List<User> listGroupUsers(String userName, String groupName) throws RemoteException;

    List<User> listAuthenticatedUsers(String userName) throws RemoteException;
}