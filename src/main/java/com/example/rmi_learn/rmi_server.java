package com.example.rmi_learn;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class rmi_server {

    public static void main(String[] args) throws RemoteException {
        try {

            System.setProperty("java.rmi.server.codebase", "http://127.0.0.1:8000/");
            MyremoteImple obj = new MyremoteImple();
            URLDownload url = new URLDownload();
            myremote services = (myremote) UnicastRemoteObject.exportObject(obj, 0);

            Registry reg;
            reg = LocateRegistry.createRegistry(10990);
            reg.rebind("Hello", services);
            reg.rebind("url", (Remote) url);

            System.err.println("Server ready");
        }catch (Exception e){
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
