package cn.jsbintask.aio.example;

import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * @author jianbin
 * @date 2020/5/28 17:23
 */
public class FileChannel_Example5_Copy3 {
    @SneakyThrows
    public static void main(String[] args) {
        // copy:  file_1.txt => file_2.txt
        FileInputStream fis = new FileInputStream("file_channel_example.txt");
        FileChannel readChannel = fis.getChannel();

        // write
        FileChannel writeChannel = new FileOutputStream("file_channel_example_copy.txt").getChannel();


        readChannel.transferTo(0, fis.available(), writeChannel);

        writeChannel.transferFrom(readChannel, fis.available(), fis.available());
    }
}
