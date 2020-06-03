package cn.jsbintask.netty.server.chatroom.server;

import cn.jsbintask.netty.server.chatroom.common.Message;
import cn.jsbintask.netty.server.chatroom.common.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author jsbintask@gmail.com
 * @date 2019/1/30 14:39
 */
public class ServerTransferMsgHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        String totalMsg = in.readCharSequence(in.readableBytes(), StandardCharsets.UTF_8).toString();
        String[] content = totalMsg.split("~");
        out.add(new Message(content[0], Utils.parseDateTime(content[1]), content[2]));
    }
}
