package cn.jsbintask.aio.example;

import lombok.SneakyThrows;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author jianbin
 * @date 2020/5/28 17:36
 */
public class MappedByteBuffer_Test1 {
    @SneakyThrows
    public static void main(String[] args) {
        RandomAccessFile accessFile = new RandomAccessFile("file_channel_example_copy.txt", "rw");
        FileChannel fileChannel = accessFile.getChannel();

        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 10);


        // 将第 0 个的字节改为  ‘Z'
        mappedByteBuffer.load();
        mappedByteBuffer.put(0, (byte) 'Z');
        mappedByteBuffer.put(9, (byte) 'U');
        mappedByteBuffer.force();
    }
}
