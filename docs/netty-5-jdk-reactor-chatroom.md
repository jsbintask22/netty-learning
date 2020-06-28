介绍了jdk实现nio的关键Selector以及SelectableChannel，了解了它的原理，就明白了netty为什么是事件驱动模型：(netty极简教程（四）：[Selector事件驱动以及SocketChannel
的使用](https://www.jianshu.com/p/63a725c0646e)，接下来将它的使用更深入一步， nio reactor模型演进以及聊天室的实现；

-----
示例源码： [https://github.com/jsbintask22/netty-learning](https://github.com/jsbintask22/netty-learning)

## nio server
对于io消耗而言，我们知道提升效率的关键在于服务端对于io的使用；而nio压榨cpu的关键在于使用`Selector`实现的`reactor`事件模型以及多线程的加入时机：

### 单线程reactor模型
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/12.png)
省略Selector以及ServerSocketChannel的获取注册； 将所有的操作至于reactor主线程
```java
 while (true) {   // 1
    if (selector.select(1000) == 0) {   // 2
        continue;
    }

    Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();    // 3
    while (selectedKeys.hasNext()) {
        SelectionKey selectionKey = selectedKeys.next();
        SelectableChannel channel = selectionKey.channel();

        if (selectionKey.isAcceptable()) {    // 4
            ServerSocketChannel server = (ServerSocketChannel) channel;
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(CLIENT_BUFFER_SIZE));
            String serverGlobalInfo = "系统消息：用户[" + client.getRemoteAddress() + "]上线了";
            System.err.println(serverGlobalInfo);

            forwardClientMsg(serverGlobalInfo, client);   //  5
        } else if (selectionKey.isReadable()) {

                SocketChannel client = (SocketChannel) channel;
                SocketAddress remoteAddress = null;
                try {
                    remoteAddress = client.getRemoteAddress();
                    String clientMsg = retrieveClientMsg(selectionKey);
                    if (clientMsg.equals("")) {
                        return;
                    }
                    System.err.println("收到用户[" + remoteAddress + "]消息：" + clientMsg);

                    forwardClientMsg("[" + remoteAddress + "]:" + clientMsg, client);   // 6
                } catch (Exception e) {
                    String msg = "系统消息：" + remoteAddress + "下线了";
                    forwardClientMsg(msg, client);            
                    System.err.println(msg);
                    selectionKey.cancel();    // 7
                    try {
                        client.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
        }

        selectedKeys.remove();
    }
}
```
1. 开启一个while循环，让Selector不断的询问操作系统是否有对应的事件已经准备好
2. Selector检查事件（等待时间为1s），如果没有直接开启下一次循环
3. 获取已经准备好的事件（`SelectionKey`)，然后依次循环遍历处理
4. 如果是`Accept`事件，说明是ServerSocketChannel注册的，说明新的连接已经建立好了，从中获取新的连接并将新连接再次注册到Selector
5. 注册后，然后生成消息给其它Socket，表示有新用户上线了
6. 如果是`Read`事件，说明客户端Socket有新的数据可读取，读取然后广播该消息到其它所有客户端
7. 如果发生异常，表示该客户端断开连接了（粗略的处理），同样广播一条消息，并且将该Socket从Selector上注销

读取以及广播消息方法如下：
```java
SocketChannel client = (SocketChannel) selectionKey.channel();
        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
        int len = client.read(buffer);
        if (len == 0) {
            return "";
        }
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        int index = 0;
        while (len != index) {
            data[index++] = buffer.get();
        }
        buffer.clear();
        return new String(data, StandardCharsets.UTF_8);
```

-----

```java
Set<SelectionKey> allClient = selector.keys();
allClient.forEach(selectionKey -> {
    SelectableChannel channel = selectionKey.channel();
    if (!(channel instanceof ServerSocketChannel) && channel != client) {  // 1
        SocketChannel otherClient = (SocketChannel) channel;
        try {
            otherClient.write(ByteBuffer.wrap(clientMsg.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
});
```
从Selector上获取所有注册的Channel然后遍历，如果不是ServerSocketChannel或者当前消息的Channel，就将消息发送出去.

-----
以上，所有代码放在同一线程中，对于单核cpu而言，相比于bio的`Socket`编程，我们主要有一个方面的改进
* 虽然`accept`方法依然是阻塞的，可是我们已经知道了肯定会有新的连接进来，所以调用改方法不会再阻塞而是直接获取一个新连接
* 对于`read`方法而言同样如此，虽然该方法依然是一个阻塞的方法，可是我们已经知道了接下来调用必定会有有效数据，这样cpu不用再进行等待
* 通过Selector在一个线程中便管理了多个Channel

-----
而对于多核cpu而言，Selector虽然能够有效规避accept和read的无用等待时间，可是它依然存在一些问题；
1. 上面的操作关键在于Selector的`select`操作，该方法必须能够快速循环调用，不宜和其它io读取写入放在一起
2. channel的io（read和write）操作较为耗时，不宜放到同一线程中处理

### 多线程reactor模型
![Reactor多线程模型](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/13.png)
基于上面的单线程问题考虑，我们可以将io操作放入线程池中处理：
1. 将accept事件的广播放入线程池中处理
2. 将read事件的所有io操作放入线程池中处理
```java
if (selectionKey.isAcceptable()) {
        ServerSocketChannel server = (ServerSocketChannel) channel;
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(CLIENT_BUFFER_SIZE));
        String serverGlobalInfo = "系统消息：用户[" + client.getRemoteAddress() + "]上线了";
        System.err.println(serverGlobalInfo);

        executorService.submit(() -> {    // 1
            forwardClientMsg(serverGlobalInfo, client);
        });
    } else if (selectionKey.isReadable()) {

        executorService.submit(() -> {    // 2
            SocketChannel client = (SocketChannel) channel;
            SocketAddress remoteAddress = null;
            try {
                remoteAddress = client.getRemoteAddress();
                String clientMsg = retrieveClientMsg(selectionKey);
                if (clientMsg.equals("")) {
                    return;
                }
                System.err.println("收到用户[" + remoteAddress + "]消息：" + clientMsg);

                forwardClientMsg("[" + remoteAddress + "]:" + clientMsg, client);  
            } catch (Exception e) {
                String msg = "系统消息：" + remoteAddress + "下线了";
                forwardClientMsg(msg, client);
                System.err.println(msg);
                selectionKey.cancel();
                try {
                    client.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    selectedKeys.remove();
}
```
在 1与2处，我们加入了线程池处理，不再在reactor主线程中做任何io操作。 这便是reactor多线程模型

----
虽然模型2有效利用了多核cpu优势，可是依然能够找到瓶颈
* 虽然广播消息是在一个独立线程中，可是我们需要将Selector上注册的所有的channel全部遍历，如果Selector注册了太多的channel，依旧会有效率问题
* 因为Selector注册了过多的Channel，所以在进行select选取时对于主线程而言依旧会有很多的循环操作，存在瓶颈

-----
基于以上问题，我们可以考虑引入多个`Selector`，这样主Selector只负责读取accept操作，而其他的io操作均有子Selector负责，这便是多Reactor多线程模型

### 多Reactor多线程模型
![Reactor多线程模型](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/14.png)

基于上面的思考，我们要在单Reactor多线程模型上主要需要以下操作
1. 对于accept到的新连接不再放入主Selector，将其加入多个`子Selector`
2. 子Selector操作应该在异步线程中进行.
3. 所有子Selector只进行read write操作

----
基于以上，会增加一个子Selector列表，并且将原来的accept以及读取广播分开；
`private List<Selector> subSelector = new ArrayList<>(8);` 定义一个包含8个子selector的列表并进行初始化
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/8.png)

-----
如图，分别开启了一个reactor主线程，以及8个子selector子线程，其中，主线程现在只进行accept然后添加至子selector
```java
 while (true) {
    if (mainSelector.select(1000) == 0) {
        continue;
    }

    Iterator<SelectionKey> selectedKeys = mainSelector.selectedKeys().iterator();
    while (selectedKeys.hasNext()) {
        SelectionKey selectionKey = selectedKeys.next();
        SelectableChannel channel = selectionKey.channel();

        if (selectionKey.isAcceptable()) {

            ServerSocketChannel server = (ServerSocketChannel) channel;
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(subSelector.get(index++), SelectionKey.OP_READ,     // 1
                    ByteBuffer.allocate(CLIENT_BUFFER_SIZE));
            if (index == 8) {   // 2
                index = 0;
            }

            String serverGlobalInfo = "系统消息：用户[" + client.getRemoteAddress() + "]上线了";
            System.err.println(serverGlobalInfo);

            forwardClientMsg(serverGlobalInfo, client);
        }
    }

    selectedKeys.remove();
}
```
1. 将新连接注册至从Selector.
2. 如果当前的selector已经全部添加了一遍则重新从第一个开始

-----
所有的从Selector只进行io操作，并且本身已经在异步线程中运行
```java
while (true) {
    if (subSelector.select(1000) == 0) {
        continue;
    }

    Iterator<SelectionKey> selectedKeys = subSelector.selectedKeys().iterator();
    while (selectedKeys.hasNext()) {
        SelectionKey selectionKey = selectedKeys.next();
        SelectableChannel channel = selectionKey.channel();

        if (selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel) channel;
            SocketAddress remoteAddress = null;
            try {
                remoteAddress = client.getRemoteAddress();
                String clientMsg = retrieveClientMsg(selectionKey);  // 1
                if (clientMsg.equals("")) {
                    return;
                }
                System.err.println("收到用户[" + remoteAddress + "]消息：" + clientMsg);
            
                forwardClientMsg("[" + remoteAddress + "]:" + clientMsg, client);  // 2
            } catch (Exception e) {
                String msg = "系统消息：" + remoteAddress + "下线了";
                forwardClientMsg(msg, client);
                System.err.println(msg);
                selectionKey.cancel();
                try {
                    client.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        selectedKeys.remove();
    }
```
1. 读取消息
2. 广播消息
启动server，并且打开三个客户端：
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/9.png)
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/10.png)
![](https://gitee.com/jsbintask/blog-static/raw/master/netty/jdk-nio/11.png)
上线通知，消息转发，下线通知成功， 主Selector与从Selector交互成功

## netty线程模型思考
事实上，在netty的线程模型中，与上方的`多Reactor多线程模型类似`，一个改进版的多路复用多Reactor模型； `Reactor主从线程模型`
1. 一个主线程不断轮询进行accept操作，将channel注册至子Selector
2. 一个线程持有一个Selector
3. 一个子Selector又可以管理多个channel
4. 在断开连接前，一个channel总是在同一个线程中进行io操作处理

-----
基于以上思考，我们将在后面在netty源码中进行一一验证。


