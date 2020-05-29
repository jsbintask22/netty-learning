package cn.jsbintask.nio.example;

import lombok.SneakyThrows;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author jianbin
 * @date 2020/5/28 16:18
 */
public class FileChannel_Example1 {
    @SneakyThrows
    public static void main(String[] args) {
        FileOutputStream fos = new FileOutputStream("file_channel_example.txt");
        FileChannel fileChannel = fos.getChannel();

        String src = "hello from jsbintask.cn zh中文\n...test";

        // write
        ByteBuffer writeBuffer = ByteBuffer.wrap(src.getBytes(StandardCharsets.UTF_8));
        fileChannel.write(writeBuffer);

        System.out.println();
    }
}
