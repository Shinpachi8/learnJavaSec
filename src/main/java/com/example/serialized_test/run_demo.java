package com.example.serialized_test;

import java.io.*;

public class run_demo {
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        User u = new User();
        u.setUsername("1222222");
        System.out.println("======= befor =======");
        System.out.println(u);
        System.out.println(u.hashCode());
        byte[] data = do_serialize(u);
        User u2 = (User)do_unserialize(data);
        System.out.println("======= after =======");
        System.out.println(u2);
        System.out.println(u2.hashCode());

        System.out.println(u2.equals(u));






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
