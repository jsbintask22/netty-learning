package cn.jsbintask;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;

/**
 * @author jianbin
 * @date 2020/6/2 9:49
 */
public class AddressTest {
    @SneakyThrows
    public static void main(String[] args) {
        System.out.println(new InetSocketAddress("127.0.0.1s", 8888));
    }
}
