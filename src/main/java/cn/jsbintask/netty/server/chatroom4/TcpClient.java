package cn.jsbintask.netty.server.chatroom4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * @author jianbin
 * @date 2020/6/1 17:44
 */
public class TcpClient extends SimpleChannelInboundHandler<ChatMessageProto.ChatMessage> {
    private Channel channel;
    private ChatMessageProto.User user;
    private CountDownLatch lock = new CountDownLatch(1);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessageProto.ChatMessage msg) throws Exception {
        ChatMessageProto.ChatMessage.MsgType msgType = msg.getMsgType();
        switch (msgType) {
            case SERVER_INFO: {
                user = msg.getTo();
                System.err.println("系统已授权身份：" + user.getUsername() + "(" + user.getAddress() + ")，您可以开始聊天了.");
                lock.countDown();
                break;
            }

            case GLOBAL: {
                System.err.println(msg.getContent());
                break;
            }

            case PRIVATE: {
                System.err.println(msg.getContent() + " [私聊消息]");
                break;
            }

            default:
                System.err.println(msg.getContent());
        }
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
    }

    @SneakyThrows
    public static void main(String[] args) {
        TcpClient tcpClient = new TcpClient();

        NioEventLoopGroup ioEventLoop = new NioEventLoopGroup(1);
        try {
            Bootstrap client = new Bootstrap()
                    .group(ioEventLoop)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new ProtobufEncoder())
                                    .addLast(new ProtobufDecoder(ChatMessageProto.ChatMessage.getDefaultInstance()))
                                    .addLast("chatRoomHandler", tcpClient);
                        }
                    });

            // 新建一个线程 不断输入控制台
            Scanner scanner = new Scanner(System.in);
            new Thread(() -> {
                while (true) {
                    loopMsg(tcpClient, scanner);
                }
            }).start();

            ChannelFuture future = client.connect(new InetSocketAddress("localhost", 9999))
                    .addListener(f -> {
                        if (f.isSuccess()) {
                            System.err.println("连接服务器成功. ");
                        } else {
                            System.err.println("连接服务器失败，请检查服务器是否启动");
                        }
                    })
                    .sync();


            future.channel().closeFuture().addListener(f -> {
                System.err.println("断开服务器成功.");
            }).sync();

        } finally {
           ioEventLoop.shutdownGracefully();
        }
    }

    private static void loopMsg(TcpClient tcpClient, Scanner scanner) {
        // 知道服务器给客户端 分配 身份前， 都不能发送消息。
        try {
            tcpClient.lock.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String msg = scanner.nextLine();
        // 用 to_username: 判断是不是私聊信息
        if (msg.startsWith("to_")) {
            int i = msg.indexOf(":", msg.indexOf(":") + 1);
            String[] identities = msg.substring(3, i).split("/");
            if (identities.length == 2) {
                tcpClient.channel.writeAndFlush(ChatMessageProto.ChatMessage.newBuilder()
                .setTo(ChatMessageProto.User.newBuilder().setUsername(identities[0]).setAddress("/" + identities[1]))
                        .setFrom(tcpClient.user)
                        .setContent(msg.substring(i + 1))
                        .setMsgType(ChatMessageProto.ChatMessage.MsgType.PRIVATE)
                        .build()
                );
                return;
            }
        }

        tcpClient.channel.writeAndFlush(ChatMessageProto.ChatMessage.newBuilder()
                .setFrom(tcpClient.user)
                .setContent(msg)
                .setMsgType(ChatMessageProto.ChatMessage.MsgType.GLOBAL)
                .build());
    }
}
