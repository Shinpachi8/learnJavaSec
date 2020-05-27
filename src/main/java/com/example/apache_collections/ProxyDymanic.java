package com.example.apache_collections;


import java.lang.reflect.*;

public class ProxyDymanic {
    /*
    对于CommonCollection, CommonCollection3, 都出现了动态代理，即Proxy.newInstance()等实例
    但是之前只对反射了解了一下， 并没有深入了解动态代理， 这里有点困惑，所以抽点时间先对动态代理做一个学习
     使用[Java动态代理](https://juejin.im/post/5ad3e6b36fb9a028ba1fee6a) 的例子来做

     1. 声明一个接口，定义了两个函数
     2. 创建一个实现该接口的类， 并在函数做一些输出
     3. 创建一个代理类， 可以对该实现类做包装，并在方法上做一些调整

     如 VendorPorxy 类, 可以由不同的Vendor作为参数， 有一点类似注解的意思。 这种叫做静态代理，因为每次它必须先声明，
     然后才可以调用。

     下边看看动态代理

     据文章描述， 需要实现一个InvocationHandler 该类是一个接口 ， 这个接口我们很熟悉了，在CommonCollection， 和 CommmonCollection2中都有实现
     AnnotationInvocationHandler 是该接口的一个实现类,

     继续InvocationHandler 还需要实现它的inovke函数， 而这个函数也是我们需要调用的

     */
    // 一个销售的接口， 包含了sell 和 ad两个方法
//    public interface Sell {
//        void sell();
//        void ad();
//    }

    public static class Vendor implements Sell {
        public void sell() {
            System.out.println("In sell method");
        }

        public void ad() {
            System.out.println("ad method");
        }
    }

    public static class VendorPorxy implements Sell{
        private Vendor vd;

        public VendorPorxy(Vendor vd){
            this.vd = vd;
        }

        @Override
        public void sell(){
            System.out.println("in proxy sell,  we add some extra output");
            this.vd.sell();

        }

        @Override
        public void ad() {

        }
    }

    public static class VendorDymanicProxy implements InvocationHandler{
        //被调用对象
        private Object obj;

        public VendorDymanicProxy(Object obj){
            this.obj = obj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println(proxy.getClass().getName());
            System.out.println("in dymanic proxy invoke method");
            Object result = method.invoke(obj, args);
            System.out.println("dymanic proxy invoke done!");
            return  result;

        }


    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Vendor vd = new Vendor();
        vd.sell();


        VendorPorxy vp = new VendorPorxy(vd);
        vp.sell();

        VendorDymanicProxy vdp = new VendorDymanicProxy(vd);
//        Sell s2 = (Sell)vdp;
//        s2.sell();
        // 据文章，加上这句话，会生成一个proxy$0.class， 这个也很常用，原来是动态代理调用生成的
        System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles","true");
        // 不能直接调用，需要传一个method的方法，还需要单独做一次反射
//        vdp.invoke(new Object(), "sell", "12");
        // 可以调用Proxy.newInstance来调用
        System.out.println(Sell.class.getClassLoader());
        Sell vdp_sell = (Sell) Proxy.newProxyInstance(Sell.class.getClassLoader(), new Class[]{Sell.class}, vdp);
        vdp_sell.sell();
        //输出了， 正常执行了，我们看一下Proxy.newInstance这个类实现什么
        //in dymanic proxy invoke method
        //In sell method
        //dymanic proxy invoke done!
        // Proxy.newInstance()创建了一个Sell的实例，然后传入了vdp作为实例化对象
        //``` return cons.newInstance(new Object[]{h});``` 然后会判断构造函数的修释符， 如果非public, 会设置setAccess
        // 而且如果是一个单独的接口，是没有InvoationHandler的构造函数的
        Class clazz = Class.forName("com.sun.proxy.$Proxy0");
        Constructor cr = clazz.getConstructor(InvocationHandler.class);
        Object obj = cr.newInstance(new Object[]{vdp});
        ((Sell) obj).sell();
        // 单独将这个类设置为 com.sun.proxy.$Proxy0，
        // 然后调用也是可以的 也就是说 动态代理demo通过，回过去看看commonCollection3
    }

}
