package cn.jsbintask.aio.example;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.stream.Stream;

/**
 * @author jianbin
 * @date 2020/5/28 17:58
 */
public class NioSocket_Test1 {
    @SneakyThrows
    public static void main(String[] args) {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(9999));

        ByteBuffer[] byteBuffers = {ByteBuffer.allocate(5), ByteBuffer.allocate(5)};

        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            long len = socketChannel.read(byteBuffers);
            System.out.println(len);
            Stream.of(byteBuffers).forEach(byteBuffer -> {
                System.out.println("pos: " + byteBuffer.position() + " limit: " + byteBuffer.limit());
                byteBuffer.flip();
                System.out.println(new String(byteBuffer.array()));
            });
        }
    }
}
