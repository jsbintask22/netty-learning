package cn.jsbintask.jdknio.example;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

/**
 * @author jianbin
 * @date 2020/5/28 17:58
 */
public class ServerSocketChannel_Test1 {
    @SneakyThrows
    public static void main(String[] args) {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(9999));

        ByteBuffer[] byteBuffers = {ByteBuffer.allocate(5), ByteBuffer.allocate(50)};

        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.write(ByteBuffer.wrap(("欢迎连接：" + socketChannel.getRemoteAddress().toString())
                    .getBytes(StandardCharsets.UTF_8)));
            while (true) {
                long len = socketChannel.read(byteBuffers);
                Stream.of(byteBuffers).forEach(byteBuffer -> {
                    System.out.println("pos: " + byteBuffer.position() + " limit: " + byteBuffer.limit());
                    byteBuffer.flip();
                    System.out.println(new String(byteBuffer.array()));
                });
                socketChannel.write(ByteBuffer.wrap("你发送了：".getBytes(StandardCharsets.UTF_8)));
                socketChannel.write(byteBuffers);
                Stream.of(byteBuffers).forEach(Buffer::clear);
                long count = 0;
            }
        }
    }
}
