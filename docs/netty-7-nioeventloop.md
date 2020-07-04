上一节我们找到了ServerSocketChannel的生成，注册Selector，绑定端口启动等等：[netty极简教程（六）：Netty是如何屏蔽ServerSocketChannel启动的](https://www.jianshu.com/p/74bd8f945daf)，  
接下来接续验证在Netty中Selector的生成使用以及我们jdk 原生工作线程再netty中是怎么启动工作的: NioEventLoopGroup

-----  
示例源码： [https://github.com/jsbintask22/netty-learning](https://github.com/jsbintask22/netty-learning)  

## NioEventLoopGroup
  ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-6/3.png)
还记得我们前面粗谈的boss以及work线程吗，它是一个线程池，既然它是一个线程池，那它内部肯定有成员变量用来访问它所持有的线程，就在`NioEventLoopGroup`的构造函数中：
  ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/2.png)
从这里我们就知道这个线程池内部有一个`SelectProvider`，接着继续往下面走：
  ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/4.png)
我们知道这里的executor肯定是null的，所以它默认构造了一个`ThreadPerTaskExecutor`
  ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/5.png)
这里值得注意的是，它没有做其他任何操作，直接就是new了一个线程`并且start`了，所以这个使用这个executor提交的任务它直接就是使用线程并且直接启动了；

-------------
接着继续往下面走，它将children全部实例化并且该children是一个NioEventLoop实例数组（将上面的executor丢了进去）；
  ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/6.png)

所以现在关键地方在于，这个NioEventLoop是什么时候往ThreadPerTaskExecutor丢了一个任务，我们继续追踪它NioEventLoop
  ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/7.png)
观察它的构造方法： 这里指的注意的是，我们在NioEventLoop的构造方法中发现了Selector已经生成,
  ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/8.png)
也就是说:`children数组多大就有多少个线程就有多少个Selector`，到这里，我们第5节用jdk写的reactor子线程都对应了一个selector在这里得以验证；

------------
我们回到上一节解析的`registerAndInit()`方法
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/9.png)
最后有一个register的步骤，我们上一节说在这个方法中注册ServerChannel到Selector，其实除此之外，还有另外的步骤：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/10.png)
由于刚启动时是在main线程中，所以当前线程不等于boss线程：
```java
public boolean inEventLoop(Thread thread) {
    return thread == this.thread;
}
```
即先走下面的
```java
eventLoop.execute(new Runnable() {
    @Override
    public void run() {
        register0(promise);
    }
});
```
最终使用executor提交开启了一个新线程（忘记的回忆一下ThreadPerTaskExecutor)：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/11.png)
最终调用`NioEventLoop.run()`方法，开启Selector的无限select操作：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/12.png)
select到准备好的事件或者任务队列中有任务时（我们一开始的Channel注册就添加到了任务队列），开始执行`processSelectedKeys`处理事件：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/13.png)
和我们写的原生jdk一样，开始遍历selectedKey，并且根据不同的事件类型在`processSelectedKey`中处理：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/14.png)
所以，如果我们进入的accept事件，说明channel是ServerSocketChannel，则执行`NioMessageUnsafe.read
()`方法，接着调用NioServerSocketChannel的doReadMessages从而接受一个新连接：
```java
@Override
protected int doReadMessages(List<Object> buf) throws Exception {
    SocketChannel ch = SocketUtils.accept(javaChannel());

    try {
        if (ch != null) {
            buf.add(new NioSocketChannel(this, ch));
            return 1;
        }
    } catch (Throwable t) {
        logger.warn("Failed to create a new channel from an accepted socket.", t);

        try {
            ch.close();
        } catch (Throwable t2) {
            logger.warn("Failed to close a socket.", t2);
        }
    }

    return 0;
}
```
最后会触发SererSocketChannel中pipeline中的read方法，此时我们在上一节已经埋好伏笔，就是`ServerBootstrapAcceptor`的read会触发，从而将新接受的连接SocketChannel再次注册到selector，并且work子线程也会开始无限循环并进行上面的操作：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/netty-7/15.png)
如图，上面的`ch.eventLoop().execute`我们已经说过会直接开启一个新线程并且执行接着再初始化我们的子Socket应该初始化的各种Handler（具体示什么后面详解）; 这样，work线程也会就会开始工作。所有对应原生JDK的启动操作步骤就全部找出；











