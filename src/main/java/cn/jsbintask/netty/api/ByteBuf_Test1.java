package cn.jsbintask.netty.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

/**
 * @author jianbin
 * @date 2020/6/1 16:39
 */
public class ByteBuf_Test1 {
    @SneakyThrows
    public static void main(String[] args) {
        // 0 <= readerIndex <= writerIndex <= capacity

        ByteBuf buffer = Unpooled.buffer(10);
        ByteBuf copiedBuffer = Unpooled.copiedBuffer("abc中文*", StandardCharsets.UTF_8);
        System.out.println(copiedBuffer);

        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.writeByte((i + 1));
        }

        for (int i = 0; i < buffer.capacity(); i++) {
            System.out.println(buffer.readByte());
        }

        for (int i = 0; i < buffer.capacity(); i++) {
            System.out.println(buffer.getByte(i));
        }

    }
}
