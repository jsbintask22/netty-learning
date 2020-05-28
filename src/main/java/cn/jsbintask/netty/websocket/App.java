package cn.jsbintask.netty.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author jsbintask@gmail.com
 * @date 2019/5/12 13:09
 */
public class App {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workGroup = new NioEventLoopGroup(10);
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        try {
            serverBootstrap
                    .channel(NioServerSocketChannel.class)
                    .group(bossGroup, workGroup)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            /* http protocol */
                            pipeline.addLast("http_msg_handler", new HttpServerCodec());
                            pipeline.addLast("big_data_handler", new ChunkedWriteHandler());
                            // 2M http content is allowed.
                            pipeline.addLast("http_msg_", new HttpObjectAggregator(2 * 1024 * 1024));

                            /* websocket protocol */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

                            /* business logic handler */
                            pipeline.addLast(new WebSocketChatHandler());
                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind(8080).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
