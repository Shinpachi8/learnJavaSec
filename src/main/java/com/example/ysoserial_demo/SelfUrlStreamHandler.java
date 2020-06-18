package com.example.ysoserial_demo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class SelfUrlStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return null;
    }


    protected synchronized InetAddress getHostAddress(URL u) {
        return null;
    }
}
