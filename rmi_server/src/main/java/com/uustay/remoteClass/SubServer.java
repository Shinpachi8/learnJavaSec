package com.uustay.remoteClass;

import com.uustay.Message;
import com.uustay.Service;

import java.io.Serializable;
import java.rmi.RemoteException;

public class SubServer implements Service, Serializable {
    @Override
    public Object sendMessage(Message msg) throws RemoteException {
        return null;
    }


    public void sayHell(String msg) throws RemoteException{
        System.out.println("Subserver");
        System.out.println(msg);
    }
}
