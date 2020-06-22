package com.example.rmi_learn;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class UserInfoImpl extends UnicastRemoteObject implements UserInfo, Serializable {
    private String username;
    private String password;
    public URLDownload urlDownload;

    protected UserInfoImpl() throws RemoteException {
    }

    @Override
    public void setUsername(String username) throws RemoteException {
        this.username = username;

    }

    @Override
    public void setPassword(String password) throws RemoteException {
        this.password = password;
    }

    @Override
    public String getUsername() throws RemoteException{
        return this.username;
    }

    @Override
    public String getPassword() throws RemoteException{
        return this.password;
    }

    @Override
    public String sayHello() throws RemoteException {
        return this.username + "says:   " + "Hello";
    }

    @Override
    public Object execute() throws RemoteException {
        this.urlDownload = new URLDownload();
        this.urlDownload.sayHell();
        return this.urlDownload;
    }

}
