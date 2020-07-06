package com.uustay;

import java.rmi.RemoteException;

public class ServiceImpl implements Service{
    @Override
    public Object sendMessage(Message msg) throws RemoteException {
        System.out.println("Client Call sendMessage.  " + msg.getMessage());
        return msg.getMessage();
    }
}
