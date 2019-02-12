package cn.itcast.core.service.user;

import cn.itcast.core.pojo.user.User;

public interface UserService {

    /**
     * 获取短信验证码
     * @param phone
     */
    public void sendCode(String phone);

  //完成用户注册
    public void add(String smscode, User user);
}
