package com.uustay;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient1 {
    public static void main(String[] args) {
        try {
            System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase", "true");
            System.setProperty("java.rmi.server.useCodebaseOnly", "false"); //7u21等以后，默认为true,如果不改为false，不会调用codebase
            //https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/javarmiproperties.html
            String str = RMIClient1.class.getClassLoader().getResource("java.policy").getFile();
            System.out.println(str);

            System.setProperty("java.security.policy", RMIClient1.class.getClassLoader().getResource("java.policy").getFile());
            SecurityManager securityManager = new SecurityManager();
            System.setSecurityManager(securityManager);


            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 10990);
            Service stub = (Service) registry.lookup("Hello");

            String msg = "client msg";
            Message msg_obj = new Message();
            msg_obj.setMessage(msg);
            Message m = (Message) stub.sendMessage(msg_obj);
            m.sayHello();


        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
