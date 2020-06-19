package cn.jsbintask.jdknio.example;

import lombok.SneakyThrows;

import java.nio.IntBuffer;

/**
 * @author jianbin
 * @date 2020/5/28 15:08
 */
public class IntBuffer_Test1 {
    @SneakyThrows
    public static void main(String[] args) {
        IntBuffer intBuffer = IntBuffer.allocate(10); // 1
        for (int i = 0; i < intBuffer.capacity(); i++) {
            intBuffer.put((i + 1));  // 2
        }

        // mark = -1;
        // postion = 0;  因为恰好 10个 位置都写满了  所以用 rewind才没有影响    flip 多了一个  limit = position;
        intBuffer.flip();  // 3
        while (intBuffer.hasRemaining()) {  // 4
            int i = intBuffer.get();  // 5
            System.out.println(i);
        }

        for (int i = 0; i < intBuffer.position(); i++) {
            System.out.println(intBuffer.get(i));
        }
    }
}
