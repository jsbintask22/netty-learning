package cn.jsbintask.netty.server.timeserver.timeclient;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author jsbintask@gmail.com
 * @date 2019/1/30 9:27
 */
public class TimeClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
        try {
            ByteBuf byteBuf = (ByteBuf) msg;
            int length = byteBuf.readableBytes();
            byte[] buff = new byte[1024];
            byteBuf.readBytes(buff, 0, length);
            System.out.println("current time: " + new String(buff, 0, length));
            ctx.close();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
