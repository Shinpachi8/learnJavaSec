package com.uustay.remoteClass;

import com.uustay.Message;

import java.io.IOException;

public class SubMessage extends Message {
    private String msg;
    public SubMessage() {
        super();
    }

    @Override
    public String getMessage() {
        System.out.println("Processing message: " + msg);
        return msg;
    }

    @Override
    public void setMessage(String msg) {
        this.msg = msg;
    }

    public void sayHell() throws IOException {
        System.out.println("sayHello:  " + this.msg);
        Runtime.getRuntime().exec("galculator");
    }
}
