package com.uustay;

import com.uustay.remoteClass.SubServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServer1 {
    public static void main(String[] args) throws RemoteException {
        try {
//            System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true");
            System.setProperty("java.rmi.server.codebase", "http://127.0.0.1:8000/");

            ServiceImpl1 obj = new ServiceImpl1();
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
