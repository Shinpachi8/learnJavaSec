package com.example.commono;

import java.io.*;

public class utils {

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
