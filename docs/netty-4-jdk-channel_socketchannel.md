介绍了nio中的channel概念以及FileChannel的使用: (netty极简教程（三）： nio Channel意义以及FileChannel使用)[https://www.jianshu.com/p/b8d08fa240e2],
接下来介绍下nio中的网络channel，SocketChannel以及Selector

-----
示例源码： [https://github.com/jsbintask22/netty-learning](https://github.com/jsbintask22/netty-learning)


## SocketChannel
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/4.png)

----
它类比bio中的`Socket`. 与FileChannel相比，它实现了`NetworkChannel`，`SelectableChannel`接口。

----
1.NetworkChannel接口代表它是一个网络字节流的连接，可以在绑定在本地网络端口进行字节流的操作； 如可以 `NetworkChannel bind(SocketAddress local)`方法用于绑定，
而`NetworkChannel setOption(SocketOption<T> name, T value)`用于设置连接和进行io操作的选项，如`SO_SNDBUF
`选项用于标识发送缓冲池的大小，只有发送的字节大小达到这个值时才会真正的发送字节流

2. SelectableChannel接口主要有两个作用；
   * 该连接支持多路复用，换句话说，它支持注册到多个`Selector`（后面介绍）上，后面可由selector询问操作系统是否有注册的事件（连接，读，写）发生，这样一个selector便可管理多个channel。
   方法`SelectionKey register(Selector sel, int ops)`注册selector以及通知事件，`SelectionKey`是一个注册抽象类，可理解为连接Channel以及Selector
   ，并且可使用该对象从selector上取消注册:`void cancel();`
   
   ----
   值得注意的是，当channel关闭后，该channel也会自动从selector上注销，而当想要主动从selector注销时，必须通过SelectionKey的cancel方法，它会等到selector下一次select
   （询问操作系统）操作时才正式注销。
   
   ------
   另外，它有一个`int validOps()`可以查看当前Channel主持的事件类型（注册时需要指定）,如SocketChannel支持的事件为，读，写，连接
   ![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/6.png)

   
   * 该channel支持异步，连接，读操作不会再阻塞当前线程:`SelectableChannel configureBlocking(boolean block)`
   
   ----
   值得注意的是，如果一个channel要注册至Selector，它必须是异步的。

## ServerSocketChannel
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/5.png)

它类比bio中的`ServerSocket`，用于服务端监听指定的端口从而获取对应的SocketChannel，它同样实现了NetworkChannel接口以及SelectChannel代表可以绑定端口以及注册到Selector
上，它只支持Accept事件（获取连接），因为它本身是无法直接发送读取字节的 `SelectionKey.OP_ACCEPT`用于监听是否有新连接建立.

----
对于配置了异步选项的ServerSocketChannel来说，它的`SocketChannel accept()`将不会再阻塞，而是直接返回null。

## Selector
`Selector`是整个nio实现非阻塞的关键，它是一个多路复用器，我们知道nio是基于事件驱动的，而这些事件从何获取感知呢？ 那就需要Selector来提供，它工作需要三部来完成事件驱动模型：
1. 创建； 可以直接通过`open()`方法来创建操作系统类型的Selector，或者手动通过`AbstractSelector openSelector()`来创建
2. 注册Channel， 只要实现了`SelectableChannel`接口都可向其注册（必须是有效事件，见上）
3. 询问操作系统，selector可通过`int select()`方法返回已经注册的Channel的有效事件个数
4. 如若在3中返回的有效事件不为0，则可调用`Set<SelectionKey> selectedKeys();`返回所有的SelectionKey（可获取Channel和Selector
），从而获取知道具体的事件类型，这样，我们不必再像bio一样调用`accept()`方法或者`read`方法直接阻塞（因为真正的读写操作还未到来），而是已经知道真正的读写buffer有效然后再进行后续操作，这样就成了一个真正的非阻塞

----
知道注意的是，虽然selectedKeys()会返回真正有效的事件，但它是以来select方法的，所以select方法也提供了阻塞与非阻塞方法：
* `int select()`会一直阻塞直到至少有一个有效的事件
* `int select(long timeout)`可设置超时时间，否则直接返回0
* `int selectNow()`直接返回，不会阻塞当前线程

## 使用
我们结合上面的分析，将SocketChannel，ServerSocketChannel，Selector组件结合起来写一个具体例子，关键在于服务端如何监听
```java
ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
serverSocketChannel.configureBlocking(false);
serverSocketChannel.bind(new InetSocketAddress(999));

Selector selector = Selector.open();  // 1

serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);  // 2


while (true) {   // 3
    if (selector.select(1000) == 0) {  // 4
        continue;
    }

    Set<SelectionKey> eventKeys = selector.selectedKeys();   // 5
    Iterator<SelectionKey> iterator = eventKeys.iterator();
    while (iterator.hasNext()) {
        SelectionKey selectionKey = iterator.next();
        SelectableChannel channel = selectionKey.channel();   // 6

        // 如果是 连接已就绪事件
        if (selectionKey.isAcceptable()) {        // 7
            ServerSocketChannel server = ((ServerSocketChannel) channel);
            SocketChannel clientChannel = server.accept();
            clientChannel.configureBlocking(false);
            // 再将 client 注册到 selector
            clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));   // 8

            // 如果是可读事件 说明是客户端的连接channel
        } else if (selectionKey.isReadable()) {  // 9
            // 可将此处代码放入先程序处理，不占用 主线程循环监听cpu时间片，  类比： netty 中的 EventLoop Work线程池
            SocketChannel client = (SocketChannel) channel;
            ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
            int len = client.read(buffer);
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            int index = 0;
            while (len != index) {
                data[index++] = buffer.get();
            }
            String clientMsg = new String(data, StandardCharsets.UTF_8);
            System.out.println("client: " + clientMsg);
            buffer.clear();
            client.write(ByteBuffer.wrap(("收到请求：" + clientMsg).getBytes(StandardCharsets.UTF_8)));
        } else if (selectionKey.isWritable()) {
            // System.out.println(selectionKey.readyOps());
        } else {
            System.out.println(selectionKey.readyOps());
        }

        iterator.remove();   // 10
    }
}
```
1. 将ServerSocketChannel绑定到本地端口，获取Selector
2. 将ServerSocketChannel注册到Selector并且注册事件是 accept
3. 开始循环使用Selector，询问操作系统
4. 询问操作系统，是否有注册的事件发生
5. 返回第4步中的有效的SelectionKey
6. 从5中的key获取对应的Channel
7. 判断事件类型，如若是accept事件  代表新的连接进来
8. 获取新的连接SocketChannel并将改Channel再次注册到Selector，注册事件是 READ
9. 因为代表客户端的SocketChannel也注册到了该Selector，所以该事件也可能是 READ 代表字节池现在可读（read可直接读取），随后向改channel写入数据表示响应
10. 每次事件读取完成后，需要把改事件剔除，否则下次会重复读取到该事件

```java
SocketChannel client = SocketChannel.open();   // 1
client.configureBlocking(false);
if (!client.connect(new InetSocketAddress("localhost", 999))) {    // 2
    if (!client.finishConnect()) {    // 3
        System.out.println("连接失败，不占用cpu资源，do other things.");
    }
}
System.out.println("连接成功。.");
ByteBuffer buffer = ByteBuffer.allocate(1024);

while (true) {
    int len = client.read(buffer);        // 4
    buffer.flip();
    byte[] data = new byte[buffer.remaining()];
    int index = 0;
    while (len != index) {
        data[index++] = buffer.get();
    }
    System.out.println("server: " + new String(data, StandardCharsets.UTF_8));
    buffer.clear();
    client.write(ByteBuffer.wrap(("你好，我是客户端：" + client.getLocalAddress() + "[" + client.hashCode() + "]" +
            new Date()).getBytes(StandardCharsets.UTF_8)));

    TimeUnit.SECONDS.sleep(2);
}
```
1. 创建channel
2. 绑定到服务端地址，因为开启了异步，所以可能连接尚在建立返回false
3. 建立稳定连接
4. 读取数据，发送数据

运行效果：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/7.png)
虽然效果与bio一样， 可是在accept与read 中确不在阻塞，其中的关键则在于 Selector

-----
还记得之前分析的bio与aio之间的区别吗， 对于同步非阻塞来说，由于Selector的事件模型使得当前线程不会在真正的有效连接或者有效数据到来之前阻塞当前线程，而Selector本身的select方法也可使用非阻塞，
这样一个Selector便可管理多个Channel，相较于bio不断开启新线程处理连接及读取事件， 它可节省很多的系统资源（线程）以及无用等待。

----
类似银行取钱业务，对于bio而言，需要一直乖乖的排队等待 无法合理利用cpu，而nio无需傻傻等待，如果当前柜台不可用则马上走人做自己的事情，只是每隔一段时间便去咨询前台是否可用。

## 总结
1. 介绍了SocketChannel作用以及用法
2. 介绍ServerSocketChannel作用以及用法
3. 讲解Selector是如何实现事件驱动的
4. 使用案例及类比