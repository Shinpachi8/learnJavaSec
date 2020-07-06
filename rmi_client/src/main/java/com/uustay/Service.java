package com.uustay;

import java.rmi.RemoteException;

public interface Service  extends java.rmi.Remote {
    Object sendMessage(Message msg) throws RemoteException;
}
