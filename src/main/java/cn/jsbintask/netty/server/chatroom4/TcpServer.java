package cn.jsbintask.netty.server.chatroom4;

import cn.hutool.core.util.RandomUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author jianbin
 * @date 2020/6/1 17:18
 * <p>
 * 在 chatroom3 的基础山加入 protobuf 编解码器，将发送的消息进行编码
 * <p>
 */
@ChannelHandler.Sharable
public class TcpServer extends SimpleChannelInboundHandler<ChatMessageProto.ChatMessage> {

    /**
     * 保存 所有的 私聊群组
     */
    private static Map<ChannelId, Map<String, Object>> userChannelMap = new ConcurrentHashMap<>();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();

        // 给客户端随机生成一个身份 （user）
        ChatMessageProto.User user = generateUser(channel);
        channel.writeAndFlush(ChatMessageProto.ChatMessage.newBuilder()
                .setMsgType(ChatMessageProto.ChatMessage.MsgType.SERVER_INFO)
                .setTo(user)
                .setContent("身份授权")
                .build());

        Map<String, Object> info = new HashMap<>(2);
        info.put("user", user);
        info.put("channel", channel);
        userChannelMap.put(channel.id(), info);

        // 告诉当前用户 现在聊天室有多少人
        String msg1 = "当前聊天室用户：[" + userChannelMap.size() + "] " + userChannelMap.values().stream().map(i -> {
            ChatMessageProto.User u = (ChatMessageProto.User) i.get("user");
            return "'" + u.getUsername() + "'(" + u.getAddress() + ")";
        }).collect(Collectors.joining(", "));

        channel.writeAndFlush(ChatMessageProto.ChatMessage.newBuilder()
                .setContent(msg1)
                .setMsgType(ChatMessageProto.ChatMessage.MsgType.GLOBAL)
                .build());

        // 告诉其他用户当前用户上线了
        String msg = printMsg(user.getUsername() + user.getAddress() + " 上线了(" + userChannelMap.size() + ")");
        System.err.println(msg);
        userChannelMap.values().forEach(c -> {
            if (c.get("channel") != channel) {
                ((Channel) c.get("channel")).writeAndFlush(ChatMessageProto.ChatMessage.newBuilder()
                        .setContent(msg)
                        .setMsgType(ChatMessageProto.ChatMessage.MsgType.GLOBAL)
                        .build());
            }
        });
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        Map<String, Object> info = userChannelMap.remove(channel.id());
        ChatMessageProto.User user = (ChatMessageProto.User) info.get("user");

        String msg = printMsg(user.getUsername() + " 下线了");
        System.err.println(msg);
        userChannelMap.values().forEach(c -> {
            Channel ch = (Channel) c.get("channel");
            ch.writeAndFlush(ChatMessageProto.ChatMessage.newBuilder()
                    .setContent(msg)
                    .setMsgType(ChatMessageProto.ChatMessage.MsgType.GLOBAL)
                    .build());
        });
        System.err.println("当前在线用户: " + userChannelMap.size());
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessageProto.ChatMessage msg) throws Exception {
        Channel channel = ctx.channel();
        // 检查消息，是否包含私聊标识
        boolean privateChat = msg.getMsgType().equals(ChatMessageProto.ChatMessage.MsgType.PRIVATE);
        if (privateChat) {
            sendPrivateMsg(msg, channel);
        } else {
            // 转发给其他人
            sendToAnother(channel, msg.getFrom().getUsername() + ": " + msg.getContent());
        }
        System.err.println(printMsg("收到" + msg.getFrom().getUsername() + "消息: " + msg.getContent()));
    }

    private void sendToAnother(Channel channel, String msg) {
        userChannelMap.values().forEach(info -> {
            Channel ch = (Channel) info.get("channel");
            if (ch != channel) {
                ch.writeAndFlush(ChatMessageProto.ChatMessage.newBuilder()
                        .setContent(msg)
                        .setMsgType(ChatMessageProto.ChatMessage.MsgType.GLOBAL)
                        .build());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("客户端出现异常断开连接: " + cause.getMessage());
    }

    // @private methods


    private void sendPrivateMsg(ChatMessageProto.ChatMessage msg, Channel self) {
        ChatMessageProto.User to = msg.getTo();
        Optional<Map<String, Object>> first = userChannelMap.values().stream().filter(info -> {
            ChatMessageProto.User toUser = (ChatMessageProto.User) info.get("user");
            return to.equals(toUser);
        }).findFirst();

        if (first.isPresent()) {
            ((Channel) first.get().get("channel")).writeAndFlush(
                    ChatMessageProto.ChatMessage.newBuilder()
                            .setFrom(msg.getFrom())
                            .setContent(printMsg(msg.getFrom().getUsername() + ":" + msg.getContent()))
                            .setMsgType(ChatMessageProto.ChatMessage.MsgType.PRIVATE)
                            .build()
            );
        } else {
            System.err.println("没找到对应的收件人. " + msg);
        }
    }


    private String printMsg(Object msg) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + msg;
    }

    private ChatMessageProto.User generateUser(Channel channel) {
        String username = RandomUtil.randomString("CLIENTclientzxvf_", 3);
        return ChatMessageProto.User.newBuilder()
                .setUsername(username)
                .setAddress(channel.remoteAddress().toString())
                .build();
    }


    @SneakyThrows
    public static void main(String[] args) {
        TcpServer tcpServer = new TcpServer();

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
                            pipeline.addLast(new ProtobufEncoder())
                                    .addLast(new ProtobufDecoder(ChatMessageProto.ChatMessage.getDefaultInstance()))
                                    .addLast("chatRoomHandler", tcpServer);

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
}
