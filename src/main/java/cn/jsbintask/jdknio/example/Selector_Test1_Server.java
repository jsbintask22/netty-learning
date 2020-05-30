package cn.jsbintask.jdknio.example;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @author jianbin
 * @date 2020/5/29 11:17
 */
public class Selector_Test1_Server {
    @SneakyThrows
    public static void main(String[] args) {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(999));

        // get selector
        Selector selector = Selector.open();

        // 将 server channel注册到selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 循环监听
        long takes = 0;
        while (true) {
            if (selector.select(1000) == 0) {
                // System.out.println("selector已经循环等了100ms了，还没有监听了事件");
                continue;
            }

            Set<SelectionKey> eventKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = eventKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                SelectableChannel channel = selectionKey.channel();

                // 如果是 连接已就绪事件
                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel server = ((ServerSocketChannel) channel);
                    SocketChannel clientChannel = server.accept();
                    clientChannel.configureBlocking(false);
                    // 再将 client 注册到 selector
                    clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

                    // 如果是可读事件 说明是客户端的连接channel
                } else if (selectionKey.isReadable()) {
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

                iterator.remove();
            }
        }
    }
}
