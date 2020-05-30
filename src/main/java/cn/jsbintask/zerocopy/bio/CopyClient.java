package cn.jsbintask.zerocopy.bio;

import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;

public class CopyClient {
    @SneakyThrows
    public static void main(String[] args) {
        // 从磁盘上读取文件
        FileInputStream fis = new FileInputStream("E:/jaychou/黑色幽默 - 周杰伦.mp3");
        Socket socket = new Socket("localhost", 7777);
        OutputStream os = socket.getOutputStream();
        int len = -1;
        int count = 0;
        byte[] buff = new byte[1024];
        System.out.println("开始上传: E:/jaychou/黑色幽默 - 周杰伦.mp3");
        long start = System.currentTimeMillis();
        while ((len = fis.read(buff)) != -1) {
            count += len;
            os.write(buff, 0, len);
        }
        // 告诉服务器 写完了
        socket.shutdownOutput();
        os.flush();

        System.out.println("上传成功，花费时间：" + (System.currentTimeMillis() - start) + "ms 总大小：" + (count / 1024D / 1024D) + "M");


        // 等5s，让服务器接受完成。
       // TimeUnit.SECONDS.sleep(500);
    }
}
