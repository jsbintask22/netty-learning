package cn.jsbintask.netty.server.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Date;

/**
 * @author jsbintask@gmail.com
 * @date 2019/5/12 13:21
 */
public class WebSocketChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final ChannelGroup WEBSOCKET_CLIENTS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String text = msg.text();
        System.out.println("received msg: " + text);

        /* send msg to others clients except itself. */
        WEBSOCKET_CLIENTS.writeAndFlush(new TextWebSocketFrame(new Date().toString() + ": " + text));
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("new channel connected. " + ctx.channel().id().asLongText());
        super.handlerAdded(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        WEBSOCKET_CLIENTS.add(ctx.channel());
        System.out.println("clients size: " + WEBSOCKET_CLIENTS.size());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        WEBSOCKET_CLIENTS.remove(ctx.channel());
        System.out.println("disconnect websocket : " + ctx.channel().id().asLongText());
        super.handlerRemoved(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }
}
