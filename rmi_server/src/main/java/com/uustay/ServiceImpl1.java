package com.uustay;

import com.uustay.remoteClass.ExploitObject;
import com.uustay.remoteClass.SubMessage;
import com.uustay.remoteClass.SubServer;

import java.io.IOException;
import java.rmi.RemoteException;

public class ServiceImpl1 implements Service{
    @Override
    public Object sendMessage(Message msg) throws IOException {
        SubMessage subMessage = new SubMessage();
        subMessage.setMessage(msg.getMessage());
        return subMessage;
    }
}
