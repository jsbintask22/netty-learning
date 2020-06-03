package cn.jsbintask.protobuf;

import lombok.SneakyThrows;

/**
 * @author jianbin
 * @date 2020/6/3 10:04
 */
public class Protobuf_Test1 {
    @SneakyThrows
    public static void main(String[] args) {
        StudentProto.Student student = StudentProto.Student.newBuilder()
                .setId(1)
                .setUsername("jianbin")
                .addEmails("jsbintask@gmail.com")
                .addEmails("jianbin@foxmail.com").build();

        System.out.println(student);
    }
}
