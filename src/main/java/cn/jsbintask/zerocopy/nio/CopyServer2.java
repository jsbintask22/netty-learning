package cn.jsbintask.zerocopy.nio;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class CopyServer2 {
    @SneakyThrows
    public static void main(String[] args) {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8888));

        while (true) {
            try {
                SocketChannel client = serverSocketChannel.accept();
                int len = -1;
                int count = 0;
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                long start = System.currentTimeMillis();
                while ((len = client.read(buffer)) != -1) {
                    count += len;
                    buffer.clear();
                }
                System.out.println("接受完成，文件大小：" + count + "bytes, 时间：" + (System.currentTimeMillis() - start) + "ms");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
