package cn.jsbintask.chatroomsocketio.client;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author jsbintask@gmail.com
 * @date 2019/2/19 15:32
 */
public class ClientSocketHandler {
    private static final int PORT = 8080;
    private static final String HOST = "localhost";

    public void run() throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(HOST, PORT));

        while (true) {
            OutputStream os = socket.getOutputStream();
            os.write("hello".getBytes());
            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) throws Exception{
        new ClientSocketHandler().run();
    }
}
