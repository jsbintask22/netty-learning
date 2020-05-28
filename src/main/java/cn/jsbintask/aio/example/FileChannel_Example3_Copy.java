package cn.jsbintask.aio.example;

import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author jianbin
 * @date 2020/5/28 16:44
 */
public class FileChannel_Example3_Copy {
    @SneakyThrows
    public static void main(String[] args) {
        // copy:  file_1.txt => file_2.txt
        FileChannel fileChannel = new FileInputStream("file_channel_example.txt").getChannel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1_0_2_4);
        int length = fileChannel.read(readBuffer);

        // write
        FileChannel writeChannel = new FileOutputStream("file_channel_example_copy.txt").getChannel();
        // 由于上面操作动了 position 指针  所以 需要flip一下
        readBuffer.flip();
        writeChannel.write(readBuffer);
    }
}
