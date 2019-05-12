package cn.jsbintask.httpserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * @author jsbintask@gmail.com
 * @date 2019/5/12 11:10
 */
public class HttpMsgHandler extends SimpleChannelInboundHandler<HttpObject> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        Channel channel = ctx.channel();
        ChannelPipeline pipeline = ctx.pipeline();
        ChannelHandler handler = ctx.handler();
        System.out.println("from " + channel.remoteAddress());

        if (msg instanceof HttpRequest) {
            // send response
            ByteBuf byteBuf = Unpooled.copiedBuffer("Hello from jsbintask 中文！.", CharsetUtil.UTF_8);
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
            response.headers().add("x-auth-token", "test_token");
            response.headers().add("content-type", "text/html;charset=utf-8");
            response.headers().add("content-length", byteBuf.readableBytes());
            response.headers().add("connection", "closed");
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.channelRegistered");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.channelUnregistered");
        super.channelUnregistered(ctx);
    }

    @Override
    protected void ensureNotSharable() {
        super.ensureNotSharable();
        System.out.println("HttpMsgHandler.ensureNotSharable");
    }

    @Override
    public boolean isSharable() {
        System.out.println("HttpMsgHandler.isSharable");
        return super.isSharable();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.handlerAdded");
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.handlerRemoved");
        super.handlerRemoved(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.channelActive");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.channelInactive");
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.channelReadComplete");
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("HttpMsgHandler.userEventTriggered");
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HttpMsgHandler.channelWritabilityChanged");
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("HttpMsgHandler.exceptionCaught");
        super.exceptionCaught(ctx, cause);
    }
}
