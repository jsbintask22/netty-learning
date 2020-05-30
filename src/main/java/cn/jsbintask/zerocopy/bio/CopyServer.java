package cn.jsbintask.zerocopy.bio;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class CopyServer {
    @SneakyThrows
    public static void main(String[] args) {
        ServerSocket serverSocket = new ServerSocket(7777);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                long start = System.currentTimeMillis();
                int len = -1;
                byte[] buff = new byte[1024];
                int count = 0;
                InputStream inputStream = socket.getInputStream();
                while ((len = inputStream.read(buff)) != -1) {
                    count += len;
                   // System.out.println(count);
                }
                System.out.println("接收完成，文件总大小：" + count + "bytes. 接收时长：" + (System.currentTimeMillis() - start));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
