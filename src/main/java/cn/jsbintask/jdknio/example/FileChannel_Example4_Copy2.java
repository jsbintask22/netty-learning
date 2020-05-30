package cn.jsbintask.jdknio.example;

import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author jianbin
 * @date 2020/5/28 17:08
 */
public class FileChannel_Example4_Copy2 {
    @SneakyThrows
    public static void main(String[] args) {
        // copy:  file_1.txt => file_2.txt
        FileChannel fileChannel = new FileInputStream("file_channel_example.txt").getChannel();

        // write
        FileChannel writeChannel = new FileOutputStream("file_channel_example_copy.txt").getChannel();

        // 只分配一块很小的 缓存 分多次读
        ByteBuffer readBuffer = ByteBuffer.allocate(3);
        int len = -1;
        while ((len = fileChannel.read(readBuffer)) != -1) {
            // 需要 flip  上面读取 移动了 position
            readBuffer.flip();
            writeChannel.write(readBuffer);

            // write 又移动了 position， 导致下次不能为-1  所以需要再次 flip
            // rewind: position = 0;      mark = -1;
            // flip:   limit = position;  position = 0;  mark = -1
            // clear:  limit = capacity;  position = 0;    mark = -1
            readBuffer.flip();
        }
    }
}
