package cn.jsbintask.jdknio.chatroom;

import lombok.SneakyThrows;

/**
 * @author jianbin
 * @date 2020/5/29 15:03
 */
public class ChatroomServer {
    @SneakyThrows
    public static void main(String[] args) {
        new ServerHandler(10000).start();
    }
}
