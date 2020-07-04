现在我们已经基本了解了netty底层使用的组件，就明白了netty为什么是事件驱动模型：(netty极简教程（四）：[netty极简教程（五）：Netty的Reactor模型演进及JDK nio聊天室实现](http://www.imooc.com/article/306207)，  
接下来追踪下netty的启动源码，验证reactor模型在netty的实现  
  
-----  
示例源码： [https://github.com/jsbintask22/netty-learning](https://github.com/jsbintask22/netty-learning)  
  
## 示例  
我们以第一节打印客户端信息的代码为例：  
```java
NioEventLoopGroup bossLoopGroup = new NioEventLoopGroup(1);  // 1
NioEventLoopGroup workLoopGroup = new NioEventLoopGroup();  // 2

try {
    ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(bossLoopGroup, workLoopGroup)
            .channel(NioServerSocketChannel.class)          // 3
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {    // 9
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof ByteBuf) {
                                System.out.println("client: " + ((ByteBuf) msg).toString(StandardCharsets.UTF_8));
                            }
                        }
                    });
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)          // 4
            .childOption(ChannelOption.SO_KEEPALIVE, true);  // 5

    ChannelFuture channelFuture = serverBootstrap.bind(port).addListener(f -> {   // 6
        System.out.println("started.");
    }).sync();

    channelFuture.channel().closeFuture().addListener(future -> 
    System.out.println("DiscardServerApp.operationComplete")).sync();   // 7
} finally {
    bossLoopGroup.shutdownGracefully();       // 8
    workLoopGroup.shutdownGracefully();
}
```
  
## 启动解析  
![image](https://upload-images.jianshu.io/upload_images/10089464-ddf44458e19275ce.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
上面这段代码将客户端发送信息打印出来，并没有回应任何消息，所以叫`DiscardServer`，因为我们使用http客户端发送，所以出现了超时现象，我们对比上一节将Netty  中使用的原生组件一一找出；首先是我们一眼就能看到的：  
![image](https://upload-images.jianshu.io/upload_images/10089464-0b48cd540aa03376.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
1. 在原生`Selector`中，我们new了一个主线程（`主Reactor线程`) 用来一直循环Selector的select操作并且注册`ServerSocketChannel`，在netty  中，我们称这个线程为`boss`线程，对应这里的`bossLoopGroup`线程组，因为我们只需要一个ServerSocketChannel，所以我们直接将该线程组数量设置为1  
2. 在原生Selector中，为了防止阻塞主线程，我们又使用了一个有`8`个线程的数组(子`Reactor线程___`)（`为什么是8？`)，并且生成了同样个数的的Selector  ，这个线程组对应我们这里的`workLoopGroup`线程组，在netty中，它默认的线程个数是`cpu核数*2`;  
![image](https://upload-images.jianshu.io/upload_images/10089464-2ea90815a7d0d4ed.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
  
说到这里，虽然我们还没有介绍`NioEventLoopGroup`，估计大家已经知道了它就是一个线程池：  
![image](https://upload-images.jianshu.io/upload_images/10089464-85ef108ef4d88942.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
3. 在netty中，不再直接使用`ServerSocketChannel`，而是netty封装的`NioServerSocketChannel`（之后介绍），它会在boss线程中生成  
4. 在原生nio中，绑定端口之前可以给ServerSocketChannel配置一些参数，这些参数在`java.net.StandardSocketOptions`中可以找到，在netty  中使用`ChannelOption`进行配置  
5. 同4，它是netty给连接的客户端socket配置参数使用  
6. 类比原生的bind方法，netty使用异步回调操作。  
7. 同样给关闭服务设置回调，并且使用 sync同步方法阻塞main线程。  
8. 必须关闭两个线程池  
  
----------  
上面8点我们直接可以在main线程中观察到，可是关键的`ServerSocketChannel`初始化，注册Selector绑定到本地端口，accept接收客户端这些代码还没有找出来；我们继续；从`serverBootstrap  
.bind(port)`开始追踪：看它们是如何在线程池中被初始化的；首先需要说明的是，我们已经知道，在原生jdk中一个连接的抽象代表是`java.nio.channels.Channel`，而在netty中，它被封装成了`io.netty.channel.Channel`，后面的介绍全部默认为netty的Channel。  
  
-------  
从bind追踪，我们可以看到两个较为关键的步骤：  
![image](https://upload-images.jianshu.io/upload_images/10089464-b18131637df164ab.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
`initAndRegister`:  
```java  
final ChannelFuture initAndRegister() {  
Channel channel = null;  
channel = channelFactory.newChannel();   // 1  
init(channel);   // 2  
ChannelFuture regFuture = config().group().register(channel); // 3  
```  
* 使用channelFactory生成了`NioServerSocketChannel`，容易知道这个channelFactory是根据我们配置的NioServerSocketChannel  使用反射调用默认构造方法生成；  
![image](https://upload-images.jianshu.io/upload_images/10089464-4301ddfb159565e1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
ok，到这里我们找到了原生jdk的`ServerSocketChannel`的生成，并且可通过`javaChannel()`方法获取；  
  
* 生成channel之后开始初始化（设置ServerSocketChannel的参数等），这里值得注意的是，每一个channel中有一个`ChannelPipeline  `对象（后面介绍该对象，每一个channel对应一个pipeline，默认构造其中生成的），接着往该pipeline中添加了一个handler（在pipeline中有一个头和尾已经确定的handler  的链表，加在链尾的前一个），这个handler是`ServerBootstrapAcceptor`，所以当有新连接进入时，继续将新SocketChannel注册到Selector上。  
  ![image](https://upload-images.jianshu.io/upload_images/10089464-dab9cd53e20c2f68.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  

* 最后会在boss线程中添加一个任务：  
  ![image](https://upload-images.jianshu.io/upload_images/10089464-b61450b6117a841b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
这样，就将ServerSocketChannel也主动注册到了Selector（注意上面的步骤是将客户端连接注册到Selector，Selector怎么得来的后面细讲）  

`doBind0:`  
最终调用`io.netty.channel.AbstractChannel.AbstractUnsafe`的bind方法，最后在unsafe中调用ServerSocket的doBind方法：  
  ![image](https://upload-images.jianshu.io/upload_images/10089464-d14bf65eca0286b2.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)  
这样，服务端的ServerSocketChannel就绑定监听端口成功了；  
  
-------  
到现在，我们已经知道了原生jdk中的reactor主线程以及io子线程在netty中的对应之处，以及ServerSocketChannel是怎么被生成并且注册到Selector  
上并且时如何绑定到端口上的；接下来还有一个关键地方我们没有找出来，就是selector的循环select是在哪里被调用了，毕竟我们再上一章就知道了操作channel全靠selector；