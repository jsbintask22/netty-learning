package cn.jsbintask.netty.server.chatroom3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author jianbin
 * @date 2020/6/1 17:18
 * <p>
 * 使用netty内部编码解码器 构造的 群聊系统。
 * 在chatroom2的基础上加上私聊
 */
@ChannelHandler.Sharable
public class TcpServer extends SimpleChannelInboundHandler<String> {
    private static DefaultChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 保存 所有的 私聊群组
     */
    private static Map<String, Map<ChannelId, Channel>> channelGroups = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        allChannels.add(channel);
        String msg = printMsg(channel.remoteAddress() + " 上线了(" + allChannels.size() + ")");
        System.err.println(msg);
        // 告诉其他人 当前用户上线了
        ctx.writeAndFlush("当前聊天室用户：[" + allChannels + "] " + allChannels.stream().map(channel1 -> "'" + channel1.remoteAddress() + "'").collect(Collectors.joining(", ")));
        allChannels.writeAndFlush(msg, ChannelMatchers.isNot(channel));
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
        // 检查消息，是否包含私聊标识
        boolean privateChat = isPrivateChat(msg);
        if (privateChat) {
            sendToAnother(msg, channel);
        } else {
            // 转发给其他人
            allChannels.writeAndFlush(printMsg(channel.remoteAddress() + ": " + msg), ChannelMatchers.isNot(channel));
        }
        System.err.println("收到" + channel.remoteAddress() + "消息: " + msg);
    }

    private void sendToAnother(String msg, Channel self) {
        String to = "/" + msg.substring(3, msg.indexOf(" "));
        Optional<Channel> first = allChannels.stream().filter(channel -> {
            return channel.remoteAddress().toString().equals(to);
        }).findFirst();

        if (first.isPresent()) {
            first.get().writeAndFlush("from_" + self.remoteAddress() + ": " + msg.substring(msg.indexOf(" ") + 1));
        } else {
            System.err.println("没找到对应的收件人. " + msg);
        }
    }

    private boolean isPrivateChat(String msg) {
        if (msg != null && msg.startsWith("to_")) {
            String to = msg.substring(3, msg.indexOf(" "));
            String[] address = to.split(":");
            if (address.length == 2 && address[0].split("\\.").length == 4) {
                try {
                    int port = Integer.parseInt(address[1]);
                    return true;
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    private String printMsg(Object msg) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + msg;
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
