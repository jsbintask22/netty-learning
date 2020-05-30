package cn.jsbintask.zerocopy.nio;

import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public class CopyClient2 {
    static int MAX = 8 * 1024 * 1024;


    @SneakyThrows
    public static void main(String[] args) {
        FileInputStream fis = new FileInputStream("E:/jaychou/黑色幽默 - 周杰伦.mp3");
        FileChannel fileChannel = fis.getChannel();
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8888));

        long count = fileChannel.size();
        long start = System.currentTimeMillis();
        // 0拷贝， 和bio做对比，没有经过 再拷贝到内存 buffer 少了一次拷贝  但是一次性最多 拷贝 8M，所以这里要分开拷贝
        if (count < MAX) {
            fileChannel.transferTo(0, count, socketChannel);
        } else {
            long temp = count;
            System.out.println("开始分段拷贝");
            int position = 0;
            while (temp > 0) {
                temp = temp - MAX;
                if (temp >= 0) {
                    fileChannel.transferTo(position, MAX, socketChannel);
                    position += MAX;
                } else {
                    fileChannel.transferTo(position, -temp, socketChannel);
                    position += -temp;
                }
            }
            socketChannel.shutdownOutput();
        }
        System.out.println("上传成功，花费时间：" + (System.currentTimeMillis() - start) + "ms 总大小：" + (count / 1024D / 1024D) + "M");

        // 等服务器读完
        TimeUnit.SECONDS.sleep(2);
    }
}
