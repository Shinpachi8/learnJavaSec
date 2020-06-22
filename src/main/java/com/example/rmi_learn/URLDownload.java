package com.example.rmi_learn;

import java.io.Serializable;
import java.rmi.Remote;

public class URLDownload implements Serializable, Remote {
    public URLDownload(){
        System.out.println("download from url no param init func");
    }


    public void sayHell(){
        System.out.println("this is from URLDownload..");
    }
}
