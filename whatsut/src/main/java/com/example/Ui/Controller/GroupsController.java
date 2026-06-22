package com.example.Ui.Controller;

import com.example.Models.Group;
import com.example.Ui.Service.RmiClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupsController {
    private final RmiClientService rmiClientService;
    private final ObservableList<Group> groups = FXCollections.observableArrayList();

    public GroupsController(RmiClientService rmiClientService) {
        this.rmiClientService = rmiClientService;
    }

    public ObservableList<Group> groups() {
        return groups;
    }

    public void refreshGroups() throws RemoteException {
        groups.setAll(rmiClientService.listGroups());
    }

    public void createGroup(String groupName) throws RemoteException {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do grupo.");
        }
        rmiClientService.createGroup(groupName.trim());
        refreshGroups();
    }

    public void createGroup(String groupName, String description, List<String> participantNames) throws RemoteException {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do grupo.");
        }
        if (participantNames == null || participantNames.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um participante.");
        }
        Set<String> unique = new HashSet<>(participantNames);
        if (unique.size() != participantNames.size()) {
            throw new IllegalArgumentException("Remova usuarios duplicados antes de criar o grupo.");
        }
        rmiClientService.createGroup(groupName.trim(), description, participantNames);
        refreshGroups();
    }

    public void joinGroup(Group group) throws RemoteException {
        if (group == null) {
            throw new IllegalArgumentException("Selecione um grupo.");
        }
        rmiClientService.requestJoinGroup(group.getName());
        refreshGroups();
    }

    public void cancelJoinRequest(Group group) throws RemoteException {
        if (group == null) {
            throw new IllegalArgumentException("Selecione um grupo.");
        }
        rmiClientService.cancelJoinRequest(group.getName());
        refreshGroups();
    }

    public void approveMember(String groupName, String userName) throws RemoteException {
        if (groupName == null || groupName.isBlank() || userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Informe grupo e usuario.");
        }
        rmiClientService.approvePendingMember(groupName.trim(), userName.trim());
        refreshGroups();
    }

    public void rejectMember(String groupName, String userName) throws RemoteException {
        if (groupName == null || groupName.isBlank() || userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Informe grupo e usuario.");
        }
        rmiClientService.rejectPendingMember(groupName.trim(), userName.trim());
        refreshGroups();
    }
}