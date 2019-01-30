package cn.jsbintask.chatroom.client;

import cn.jsbintask.chatroom.common.Constants;
import cn.jsbintask.chatroom.common.Message;
import cn.jsbintask.chatroom.common.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Scanner;

/**
 * @author jsbintask@gmail.com
 * @date 2019/1/30 14:38
 */
public class ClientMsgHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        try {
            Utils.printMsg(msg);
            Scanner scanner = new Scanner(System.in);
            System.out.print("jsbintask-client, please input msg: ");
            String reply = scanner.nextLine();


            Message message = new Message(Constants.CLIENT, new Date(), reply);
            ByteBuf buffer = ctx.alloc().buffer();
            String content = message.getUsername() + "~" + Utils.formatDateTime(message.getSentTime()) + "~" + message.getMsg();
            buffer.writeBytes(content.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(buffer);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

}
