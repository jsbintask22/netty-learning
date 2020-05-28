package cn.jsbintask.bio.rpc;

/**
 * @author jianbin
 * @date 2020/5/18 16:23
 */
public interface UserService  {
    User findById(Integer id);

    void delete(User user);
}
