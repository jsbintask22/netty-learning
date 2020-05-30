package cn.jsbintask.jdknio.chatroom;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author jianbin
 * @date 2020/5/29 15:38
 */
public class ClientHandler {
    private ExecutorService works;
    private InetSocketAddress server;

    @SneakyThrows
    public ClientHandler(InetSocketAddress server) {
        works = Executors.newCachedThreadPool();
        this.server = server;
    }

    @SneakyThrows
    public void start() {
        SocketChannel client = SocketChannel.open();
        client.configureBlocking(false);
        if (!client.connect(server)) {
            if (!client.finishConnect()) {
                System.err.println("连接服务器: " + server + "失败，请检查服务是否启动.");
            }
        }
        System.err.println("连接成功。.");
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        boolean inputing = true;

        while (true) {
            printServerMsg(client, buffer);

            if (inputing) {
                works.submit(() -> {
                    try {
                        while (true) {
                            Scanner scanner = new Scanner(System.in);
                            String clientMsg = client.getRemoteAddress() + ": ";
                           // System.out.print(clientMsg);
                            clientMsg = scanner.nextLine();
                            client.write(ByteBuffer.wrap(clientMsg.getBytes(StandardCharsets.UTF_8)));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                inputing = false;
            }

            TimeUnit.SECONDS.sleep(1);
        }
    }

    private void printServerMsg(SocketChannel client, ByteBuffer buffer) throws IOException {
        int len = client.read(buffer);
        if (len > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            int index = 0;
            while (len != index) {
                data[index++] = buffer.get();
            }
            buffer.clear();
            System.err.println(new String(data, StandardCharsets.UTF_8));
        }
    }

}
