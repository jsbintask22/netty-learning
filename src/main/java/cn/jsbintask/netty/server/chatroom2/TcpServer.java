package cn.jsbintask.netty.server.chatroom2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * @author jianbin
 * @date 2020/6/1 17:18
 */
@ChannelHandler.Sharable
public class TcpServer extends SimpleChannelInboundHandler<String> {
    private static DefaultChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String msg = printMsg(channel.remoteAddress() + " 上线了");
        System.err.println(msg);
        // 告诉其他人 当前用户上线了
        ctx.writeAndFlush("当前聊天室用户：[" + allChannels.size() + "] " + allChannels.stream().map(channel1 -> {
            if (channel1 != channel) {
                return "'" + channel.remoteAddress() + "'";
            } else {
                return "";
            }
        }).collect(Collectors.joining(", ")));
        allChannels.writeAndFlush(msg);
        allChannels.add(channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String msg = printMsg(channel.remoteAddress() + " 下线了");
        System.err.println(msg);
        allChannels.writeAndFlush(msg);
        System.err.println("当前在线用户: " + allChannels.size());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Channel channel = ctx.channel();
        System.err.println("收到" + channel.remoteAddress() + "消息: " + msg);
        // 转发给其他人
        allChannels.writeAndFlush(printMsg(channel.remoteAddress() + ": " + msg), ChannelMatchers.isNot(channel));
    }

    private String printMsg(Object msg) {
        return new SimpleDateFormat("hh:MM:ss").format(new Date()) + " " + msg;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("客户端出现异常断开连接: " + cause.getMessage());
    }

    @SneakyThrows
    public static void main(String[] args) {
        TcpServer tcpServer = new TcpServer();

        NioEventLoopGroup acceptLoopGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup ioLoopGroup = new NioEventLoopGroup();

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
                        pipeline.addLast("stringDecoder", new StringDecoder())
                                .addLast("stringEncoder", new StringEncoder())
                                .addLast("chatRoomHandler", tcpServer);

                    }
                });

        ChannelFuture sync = serverBootstrap.bind(9999).addListener(future -> {
            System.err.println("server started.");
        }).sync();

        sync.channel().closeFuture().addListener(future -> {
            System.out.println("server shutdown.");
        }).sync();
    }
}
