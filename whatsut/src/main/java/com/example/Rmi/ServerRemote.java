package com.example.Rmi;

import com.example.Models.FileMessage;
import com.example.Models.Group;
import com.example.Models.Message;
import com.example.Models.TextMessage;
import com.example.Models.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerRemote extends Remote {
    User registerUser(String userName, String password) throws RemoteException;
    boolean login(String userName, String password, ClientRemote clientRemote) throws RemoteException;
    void logout(String userName) throws RemoteException;

    Group createGroup(String groupName, String adminName) throws RemoteException;

    boolean requestJoinGroup(String groupName, String userName) throws RemoteException;
    boolean approvePendingMember(String groupName, String adminName, String userName) throws RemoteException;

    boolean deleteUserFromGroup(String groupName, String adminName, String userName) throws RemoteException;

    boolean sendPrivateTextMessage(String Sender, String Receiver, TextMessage textMessage) throws RemoteException;
    boolean sendPrivateFileMessage(String Sender, String Receiver, FileMessage fileMessage) throws RemoteException;

    boolean sendGroupTextMessage(String groupName, String senderName, TextMessage textMessage) throws RemoteException;
    boolean sendGroupFileMessage(String groupName, String senderName, FileMessage fileMessage) throws RemoteException;

    List<Message> getMessages(String groupName, String userName) throws RemoteException;
    List<Message> getPrivateMessages(String Sender, String Receiver) throws RemoteException;
    
    List<Group> listGroups(String userName) throws RemoteException;
    List<User> listGroupUsers(String userName, String groupName) throws RemoteException;
    List<User> listUsers(String userName) throws RemoteException;
    List<User> listAuthenticatedUsers(String userName) throws RemoteException;
}