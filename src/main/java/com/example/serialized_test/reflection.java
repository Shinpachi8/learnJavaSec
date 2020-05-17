package com.example.serialized_test;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;

public class reflection {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException,
            InstantiationException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException {
        Class class_1 = Class.forName("com.example.serialized_test.User");

//第二种方法
        User u = new User();
        Class class_2 = u.getClass();

//第三种,通过类名来获取class
        Class class_3 = User.class;
        System.out.println("is class_1 = class_2: ");
        System.out.println(class_1.equals(class_2));

        System.out.println("is class_1 = class_3: ");
        System.out.println(class_1.equals(class_3));

        Constructor[] cons = class_1.getDeclaredConstructors();
        for(int i=0; i<cons.length; i++){
            System.out.println(cons[i].getName());
            Class[] params = cons[i].getParameterTypes();
            for(int j=0; j<params.length; j++){
                System.out.println("param[" + Integer.toString(j) + "]" + params[j].getName());
            }
        }


        //获取方法
        Method[] methods = class_1.getMethods();
        for(int i=0; i<methods.length; i++){
            System.out.println("--------------------------");
            System.out.println(methods[i].getName());
            Class[] params = methods[i].getParameterTypes();
            for(int j=0; j<params.length; j++){
                System.out.println("param[" + Integer.toString(j) + "]" + params[j].getName());
            }
            System.out.println("--------------------------");
        }

        User u1 = new User();
        u1.setUsername("shinpachi");
        System.out.println("befor invoke:  username=" + u1.getUsername());
        Method m1 = class_1.getMethod("setUsername", String.class);
        m1.invoke(u1, "modify_shinpachi");
        System.out.println("after invoke: username=" + u1.getUsername());


        Method m2 = class_1.getDeclaredMethod("sayLove", String.class);
        m2.setAccessible(true);
        m2.invoke(u1, "love..");


        //fields

        System.out.println("### getFields:\t");
        Field[] fields = class_1.getFields();
        for(int i=0; i<fields.length; i++){
            System.out.println(fields[i].getName());
        }
        System.out.println("----------------\n### getDeclaredFields:\t");
        Field[] fields2 = class_1.getDeclaredFields();
        for(int i=0; i<fields2.length; i++){
            System.out.println(fields2[i].getName());
        }

        System.out.println("before: " + u1.toString());
        Field field = class_1.getDeclaredField("password");
        field.setAccessible(true);
        field.set(u1, "password_set_by_reflection");
        System.out.println("after: " + u1.toString());



    }
}
