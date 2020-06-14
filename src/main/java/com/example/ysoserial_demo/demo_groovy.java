package com.example.ysoserial_demo;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.MethodClosure;

import java.io.*;

public class demo_groovy {


    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        String str = "test";
//        String cmd = "galculator";
//        MethodClosure methodClosure = new MethodClosure(str, "execute");
//        ((Closure)methodClosure).call(cmd);

        File f = new File("/home/uustay/test_groovy.ser");
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
        objectInputStream.readObject();

    }



    public static byte[]  do_serialize(Object obj) throws IOException {
        ByteArrayOutputStream btout = new ByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream(btout);
        oos.writeObject(obj);
        System.out.println("do serialized success");
        return btout.toByteArray();
    }

    public static Object do_unserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream btin = new ByteArrayInputStream(data);
        ObjectInputStream ooi = new ObjectInputStream(btin);

        System.out.println("do unserialized success");
        return ooi.readObject();
    }
}
