package cn.jsbintask.netty.server.heatbeat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;

/**
 * @author jianbin
 * @date 2020/6/2 10:37
 * <p>
 * 用于处理 空闲事件  读 写 读写空闲
 */
@ChannelHandler.Sharable
public class HeatbeatServer extends ChannelInboundHandlerAdapter {

    @SneakyThrows
    public static void main(String[] args) {
        HeatbeatServer tcpServer = new HeatbeatServer();

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
                            pipeline.addLast("logHandler", new LoggingHandler(LogLevel.TRACE))
                                    // 读空闲 3s  写空闲 5s  读写空闲  7s
                                    .addLast("idleHandler", new IdleStateHandler(3, 5, 7))
                                    .addLast("heatbeat", tcpServer)
                                    .addLast(new Handler2());
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
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Channel channel = ctx.channel();
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            System.out.println(channel.remoteAddress() + " event: " + event.state());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.err.println(ctx.channel().remoteAddress() + ": " + msg.toString());
    }
}
