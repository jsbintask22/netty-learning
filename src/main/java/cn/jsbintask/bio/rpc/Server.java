package cn.jsbintask.bio.rpc;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * @author jianbin
 * @date 2020/5/18 16:07
 */
public class Server {
    private static ExecutorService works = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(9999);

        while (true) {
            Socket client = server.accept();
            works.submit(() -> {
                try {
                    process(client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void process(Socket client) throws Exception {
        InputStream is = client.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);

        // class
        Class<?> interfacz = (Class<?>) ois.readObject();

        // method
        String methodName = ois.readUTF();

        // args
        Object[] args = (Object[]) ois.readObject();
        Class<?>[] argTypes = Stream.of(args).map(c -> c.getClass()).toArray((IntFunction<Class<?>[]>) Class[]::new);

        // invoke
        String implName = interfacz.getName() + "Impl";
        Class<?> implClass = Class.forName(implName);
        Object target = implClass.newInstance();
        Method method = implClass.getMethod(methodName, argTypes);
        Object result = method.invoke(target, args);

        ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
        oos.writeObject(result);
        oos.flush();
    }
}
