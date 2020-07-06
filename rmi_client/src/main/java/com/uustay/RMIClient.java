package com.uustay;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient {
    public static void main(String[] args) {
        try {
            Context context = new InitialContext();

            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 10990);
            Service stub = (Service) registry.lookup("Hello");

            String msg = "client msg";
            Message msg_obj = new Message();
            msg_obj.setMessage(msg);
            stub.sendMessage(msg_obj);
            System.out.println(msg);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
