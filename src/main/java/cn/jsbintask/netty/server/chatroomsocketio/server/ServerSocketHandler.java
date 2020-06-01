package cn.jsbintask.netty.server.chatroomsocketio.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jsbintask@gmail.com
 * @date 2019/2/19 15:20
 */
@SuppressWarnings("AlibabaAvoidManuallyCreateThread")
public class ServerSocketHandler {
    private static final int PORT = 8080;
    private Set<Socket> clients = new HashSet<>();

    public void run() throws Exception{
        ServerSocket serverSocket = new ServerSocket(PORT);

        Socket client = serverSocket.accept();
        clients.add(client);

        //new threads
        new Thread(() -> {
            try {
                InputStream is = client.getInputStream();
                byte[] buff = new byte[1024];
                int len;

                while ((len = is.read(buff)) != -1) {
                    System.out.println("client msg: " + new String(buff, 0, len));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) throws Exception{
        new ServerSocketHandler().run();
    }
}
