package cn.jsbintask.netty.server.stickybag;

import cn.hutool.core.util.RandomUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author jianbin
 * @date 2020/6/1 17:44
 */
public class StickyBagClient extends SimpleChannelInboundHandler<ByteBuf> {
    private int count;

    @SneakyThrows
    public static void main(String[] args) {
        StickyBagClient stickyBagClient = new StickyBagClient();
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        try {
            Bootstrap client = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline
                                    .addLast(stickyBagClient);
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
            }).sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for (int i = 0; i < 10; i++) {
            String msg = "hello_" + RandomUtil.randomString(2) + "_" + (i + 1);
            System.err.println("正在发送" + msg);
            ctx.writeAndFlush(Unpooled.copiedBuffer(msg, StandardCharsets.UTF_8));
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println(++count);
        System.out.println(msg.toString(StandardCharsets.UTF_8));
    }
}
