package com.example.rmi_learn;

import com.example.serialized_test.User;
import sun.rmi.registry.RegistryImpl;

import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MyremoteImple  implements myremote, Serializable {

    public MyremoteImple() throws RemoteException {super();};

    @Override
    public String sayHello() throws RemoteException {
        return "hello rmi world";
    }

    @Override
    public UserInfo getUser(String username) throws RemoteException {
        UserInfo u = new UserInfoImpl();
        u.setUsername(username);
        return u;
    }

    public void do_other(URLDownload urlDownload) throws RemoteException{
        urlDownload.sayHell();
    }

}
