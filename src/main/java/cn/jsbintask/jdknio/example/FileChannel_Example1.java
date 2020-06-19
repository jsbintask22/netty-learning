package cn.jsbintask.jdknio.example;

import lombok.SneakyThrows;

import java.io.FileOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author jianbin
 * @date 2020/5/28 16:18
 */
public class FileChannel_Example1 {
    @SneakyThrows
    public static void main(String[] args) {
        // 方法1， 适合文件不存在的情况
        FileOutputStream fos = new FileOutputStream("file_channel_example.txt");
        FileChannel fileChannel = fos.getChannel();

        // 方法2，
       /* FileChannel fileChannel = FileChannel.open(Paths.get("", "file_channel_example.txt"),
                StandardOpenOption.WRITE,
                StandardOpenOption.READ);*/

        String src = "hello from jsbintask.cn zh中文\n...test";

        // write
        ByteBuffer writeBuffer = ByteBuffer.wrap(src.getBytes(StandardCharsets.UTF_8));
        fileChannel.write(writeBuffer);

        System.out.println();
    }
}
