package cn.jsbintask.bio.rpc;

/**
 * @author jianbin
 * @date 2020/5/18 16:24
 */
public class UserServiceImpl implements UserService {
    @Override
    public User findById(Integer id) {
        return new User("name->" + id, id);
    }

    @Override
    public void delete(User user) {
        System.out.println("deleted: " + user);
    }
}
