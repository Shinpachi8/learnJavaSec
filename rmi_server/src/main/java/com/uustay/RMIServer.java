package com.uustay;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServer {

    public static void main(String[] args) throws RemoteException {
        try {

            ServiceImpl obj = new ServiceImpl();
            Service services = (Service) UnicastRemoteObject.exportObject( obj, 0);

            Registry reg;
            reg = LocateRegistry.createRegistry(10990);
            reg.rebind("Hello", services);

            System.err.println("Server ready");
        }catch (Exception e){
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
