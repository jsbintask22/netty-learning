package cn.jsbintask.nio.chatroom;

import lombok.Data;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author jianbin
 * @date 2020/5/29 15:06
 */
@Data
public class ServerHandler {
    private Selector selector;
    private int port;
    private ExecutorService executorService;
    private static final int CLIENT_BUFFER_SIZE = 1024;

    @SneakyThrows
    public ServerHandler(int port) {
        this.port = port;
        executorService = Executors.newCachedThreadPool();
    }

    @SneakyThrows
    public void start() {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));

        // get selector
        selector = Selector.open();

        // 将 server channel注册到selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.err.println("server started!");

        listenClientEvents();
    }

    @SneakyThrows
    private void listenClientEvents() {
        while (true) {
            if (selector.select(1000) == 0) {
                continue;
            }

            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey selectionKey = selectedKeys.next();
                SelectableChannel channel = selectionKey.channel();

                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) channel;
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(CLIENT_BUFFER_SIZE));
                    String serverGlobalInfo = "系统消息：用户[" + client.getRemoteAddress() + "]上线了";
                    System.err.println(serverGlobalInfo);

                    executorService.submit(() -> {
                        forwardClientMsg(serverGlobalInfo, client);
                    });
                } else if (selectionKey.isReadable()) {

                    executorService.submit(() -> {
                        SocketChannel client = (SocketChannel) channel;
                        SocketAddress remoteAddress = null;
                        try {
                            remoteAddress = client.getRemoteAddress();
                            String clientMsg = retrieveClientMsg(selectionKey);
                            if (clientMsg.equals("")) {
                                return;
                            }
                            System.err.println("收到用户[" + remoteAddress + "]消息：" + clientMsg);

                            // forward
                            // 此处还可开启一个 线程池   防止阻塞 服务端专门处理消息的线程
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

            TimeUnit.SECONDS.sleep(1);
        }
    }

    @SneakyThrows
    private void forwardClientMsg(String clientMsg, Channel client) {
        Set<SelectionKey> allClient = selector.keys();
        allClient.forEach(selectionKey -> {
            SelectableChannel channel = selectionKey.channel();
            // dont need forward itself.
            if (!(channel instanceof ServerSocketChannel) && channel != client) {
                SocketChannel otherClient = (SocketChannel) channel;
                try {
                    otherClient.write(ByteBuffer.wrap(clientMsg.getBytes(StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SneakyThrows
    private String retrieveClientMsg(SelectionKey selectionKey) {
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
    }
}
