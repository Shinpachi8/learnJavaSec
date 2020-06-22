package com.example.rmi_learn;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UserInfo extends Remote, Serializable {

    void setUsername(String username) throws RemoteException;
    void setPassword(String password) throws RemoteException;

    String getUsername() throws RemoteException;
    String getPassword() throws RemoteException;

    String sayHello() throws RemoteException;

    Object execute() throws RemoteException;

}
