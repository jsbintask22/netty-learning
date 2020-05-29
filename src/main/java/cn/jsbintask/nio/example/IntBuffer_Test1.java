package cn.jsbintask.nio.example;

import lombok.SneakyThrows;

import java.nio.IntBuffer;

/**
 * @author jianbin
 * @date 2020/5/28 15:08
 */
public class IntBuffer_Test1 {
    @SneakyThrows
    public static void main(String[] args) {
        //  mark <= position <= limit <= capacity.
        IntBuffer intBuffer = IntBuffer.allocate(10);
        for (int i = 0; i < intBuffer.capacity() - 2; i++) {
            intBuffer.put(i, (i + 1));
        }

       // intBuffer.put(100);

        // mark = -1;
        // postion = 0;  因为恰好 10个 位置都写满了  所以用 rewind才没有影响    flip 多了一个  limit = position;
        intBuffer.rewind();
        while (intBuffer.hasRemaining()) {
            int i = intBuffer.get();
            System.out.println(i);
        }

        intBuffer.rewind();
        while (intBuffer.hasRemaining()) {
            int i = intBuffer.get();
            System.out.println(i);
        }
    }
}
