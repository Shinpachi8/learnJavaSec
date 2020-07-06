package com.uustay;

import java.io.Serializable;
import java.rmi.Remote;

public class Message implements Serializable, Remote {
    private static final long serialVersionUID = -6210579029160025375L;
    private String msg;

    public Message() {
    }

    public String getMessage() {
        System.out.println("Processing message: " + msg);
        return msg;
    }

    public void setMessage(String msg) {
        this.msg = msg;
    }
}