package cn.jsbintask.netty.server.chatroom3;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * @author jianbin
 * @date 2020/6/1 17:44
 */
public class TcpClient extends SimpleChannelInboundHandler<String> {
    private Channel channel;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.err.println(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
    }

    @SneakyThrows
    public static void main(String[] args) {
        TcpClient tcpClient = new TcpClient();

        Bootstrap client = new Bootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("stringDecoder", new StringDecoder())
                                .addLast("stringEncoder", new StringEncoder())
                                .addLast("chatRoomHandler", tcpClient);
                    }
                });

        ChannelFuture future = client.connect(new InetSocketAddress("localhost", 9999))
                .addListener(f -> {
                    if (f.isSuccess()) {
                        System.err.println("连接服务器成功.");
                    } else {
                        System.err.println("连接服务器失败，请检查服务器是否启动");
                    }
                })
                .sync();



        future.channel().closeFuture().addListener(f -> {
            System.err.println("断开服务器成功.");
        });

        // 新建一个线程 不断输入控制台
        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            while (true) {
                String msg = scanner.nextLine();
                tcpClient.channel.writeAndFlush(msg);
            }
        }).start();
    }
}
