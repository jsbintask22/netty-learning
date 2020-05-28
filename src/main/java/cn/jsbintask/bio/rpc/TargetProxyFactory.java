package cn.jsbintask.bio.rpc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.Socket;

/**
 * @author jianbin
 * @date 2020/5/18 16:27
 */
public class TargetProxyFactory {

    @SuppressWarnings("all")
    public static <T> T proxy(Class<T> s) throws Exception {
        return  (T) Proxy.newProxyInstance(s.getClassLoader(), new Class[]{s}, getJdkEnhance());
    }

    private static InvocationHandler getJdkEnhance() throws IOException {
        return (proxy, method, args) -> {
            System.out.println(proxy.getClass());
            // config
            Socket client = new Socket("localhost", 9999);

            OutputStream os = client.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);

            // write target interface
            oos.writeObject(proxy.getClass().getInterfaces()[0]);
            // write method name
            oos.writeUTF(method.getName());
            // write args
            oos.writeObject(args);
            oos.flush();
            os.flush();

            // read result
            ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
            return ois.readObject();
        };
    }
}
