package cn.jsbintask.nio.chatroom;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;

/**
 * @author jianbin
 * @date 2020/5/29 16:00
 */
public class Client {
    @SneakyThrows
    public static void main(String[] args) {
        new ClientHandler(new InetSocketAddress("127.0.0.1", 10000)).start();
    }
}
