package cn.jsbintask.nio.example;

import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author jianbin
 * @date 2020/5/28 16:26
 */
public class FileChannel_Example2 {
    @SneakyThrows
    public static void main(String[] args) {
        FileInputStream fis = new FileInputStream("file_channel_example.txt");
        FileChannel fileChannel = fis.getChannel();

        // read
        ByteBuffer readBuffer = ByteBuffer.allocate(100);
        int length = fileChannel.read(readBuffer);
        // method 1
        System.out.println(new String(readBuffer.array()));

        // method 2
        // 这里不能用 rewind  详见 IntBuffer_Test1
        readBuffer.flip();
        byte[] data = new byte[length];
        int index = 0;
        while (readBuffer.hasRemaining()) {
            data[index++] = readBuffer.get();
        }
        System.out.println(new String(data));
    }
}
