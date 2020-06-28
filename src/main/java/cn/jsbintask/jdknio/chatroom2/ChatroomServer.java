package cn.jsbintask.jdknio.chatroom2;

import lombok.SneakyThrows;

/**
 * @author jianbin
 * @date 2020/5/29 15:03
 *
 * 多Reactor多线程模型
 */
public class ChatroomServer {
    @SneakyThrows
    public static void main(String[] args) {
        new ServerHandler(10000).start();
    }
}
