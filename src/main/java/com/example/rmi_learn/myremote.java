package com.example.rmi_learn;

import com.example.serialized_test.User;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface myremote extends Remote {
    String sayHello() throws RemoteException;

    UserInfo getUser(String username) throws RemoteException;
}
