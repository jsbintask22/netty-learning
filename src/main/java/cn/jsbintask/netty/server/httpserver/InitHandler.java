package cn.jsbintask.netty.server.httpserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * @author jianbin
 * @date 2020/6/1 15:50
 */
public class InitHandler extends ChannelInitializer<NioSocketChannel> {
    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("httpCoder", new HttpServerCodec());
        pipeline.addLast("httpMsgHandler", new HttpMsgHandler());
    }
}
