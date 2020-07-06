package com.uustay;

import java.io.IOException;
import java.rmi.RemoteException;

public interface Service  extends java.rmi.Remote {
    Object sendMessage(Message msg) throws IOException;
}
