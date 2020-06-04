package cn.jsbintask.netty.server.stickybag;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

/**
 * @author jianbin
 * @date 2020/6/2 10:37
 * <p>
 * 测试 tcp再发送过程中的 粘包， 客户端一次性发送多个，服务端是否能读到同样个数量的包
 */
@ChannelHandler.Sharable
public class StickyBagServer extends SimpleChannelInboundHandler<ByteBuf> {
    private int count = 0;

    @SneakyThrows
    public static void main(String[] args) {
        StickyBagServer tcpServer = new StickyBagServer();

        NioEventLoopGroup acceptLoopGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup ioLoopGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(acceptLoopGroup, ioLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    // 服务器允许多少个 tcp 连接等待
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline

                                    .addLast(tcpServer)
                                    .addLast(new StringEncoder2())
                            ;
                        }
                    });

            ChannelFuture sync = serverBootstrap.bind(9999).addListener(future -> {
                System.err.println("server started.");
            }).sync();

            sync.channel().closeFuture().addListener(future -> {
                System.out.println("server shutdown.");
            }).sync();
        } finally {
            acceptLoopGroup.shutdownGracefully();
            ioLoopGroup.shutdownGracefully();
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println(++count);
        String x = msg.toString(StandardCharsets.UTF_8);
        System.out.println(x);
        // ctx.writeAndFlush(Unpooled.copiedBuffer(x, StandardCharsets.UTF_8));
      //  ctx.writeAndFlush(x);
        // 请观察使用 channel 和 ctx 的writeAndFlush的区别
        ctx.channel().writeAndFlush(x);
    }

}
