package com.example.serialized_test;

import java.io.Serializable;

public class User  implements Serializable {
    private static final long serialVersionUID = 8120832682080437368L;

    public String username;
    private String password;

    public User(){
        System.out.println("在构造函数中");
    }

    public User(String username){
        System.out.println("在构造函数中");
        this.username = username;
    }


    public String getUsername() {
        System.out.println("in set username methood");
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        System.out.println("在setting方法中");
        this.username = username;
    }

    private void sayLove(String string){
        System.out.println("User => username=" + this.username + "\t sayLove=> " + string);
    }

    public void setPassword(String password) {

        System.out.println("在setting方法中");
        this.password = password;
    }

    @Override
    public String toString() {
        return "User=>  username=" + this.username + "\tpassword=" + this.password;
    }
}
