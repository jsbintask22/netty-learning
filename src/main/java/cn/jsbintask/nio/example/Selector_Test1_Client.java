package cn.jsbintask.nio.example;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author jianbin
 * @date 2020/5/29 14:01
 */
public class Selector_Test1_Client {
    @SneakyThrows
    public static void main(String[] args) {
        SocketChannel client = SocketChannel.open();
        client.configureBlocking(false);
        if (!client.connect(new InetSocketAddress("localhost", 999))) {
            if (!client.finishConnect()) {
                System.out.println("连接失败，不占用cpu资源，do other things.");
            }
        }
        System.out.println("连接成功。.");
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (true) {
            int len = client.read(buffer);
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
    }
}
