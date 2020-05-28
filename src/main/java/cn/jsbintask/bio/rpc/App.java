package cn.jsbintask.bio.rpc;

/**
 * @author jianbin
 * @date 2020/5/18 16:39
 */
public class App {
    public static void main(String[] args) throws Exception {
        UserService proxy = TargetProxyFactory.proxy(UserService.class);
        User user = proxy.findById(1);
        System.out.println(user);

        proxy.delete(user);
    }
}
