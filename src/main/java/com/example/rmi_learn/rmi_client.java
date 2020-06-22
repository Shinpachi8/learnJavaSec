package com.example.rmi_learn;

import com.example.serialized_test.User;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class rmi_client {
    private rmi_client() {}

    public static void main(String[] args) {

        try {
            Context context = new InitialContext();

            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 10990);
            myremote stub = (myremote) registry.lookup("Hello");

            String hello = stub.sayHello();
            System.out.println(hello);

            UserInfo userInfo = stub.getUser("shinpachi8");
            String hello2 = userInfo.sayHello();
            System.out.println(hello2);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
