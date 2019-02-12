package cn.itcast.core.controller.user;

import cn.itcast.core.entity.Result;
import cn.itcast.core.pojo.user.User;
import cn.itcast.core.service.user.UserService;

import cn.itcast.core.utils.phone.PhoneFormatCheckUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.annotation.Target;

@RestController
@RequestMapping("/user")
public class UserController {

    @Reference
    private UserService userService;



    /**
     * 获取短信验证码
     * @param phone
     * @return
     */
    @RequestMapping("/sendCode.do")
    public Result sendCode(String phone){
        try {
            //校验手机是符合法
            boolean phoneLegal = PhoneFormatCheckUtils.isPhoneLegal(phone);
            if (!phoneLegal){
              return   new Result(false,"手机不符合");
            }
            userService.sendCode(phone);
            return new Result(true, "短信发送成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "短信发送失败");
        }
    }
    //用户注册
    @RequestMapping("/add.do")
    public Result add(String smscode,@RequestBody User user){
        try {
            userService.add(smscode,user);
            return new Result(true,"注册成功");
        } catch (RuntimeException e) {
            e.printStackTrace();
          return new Result(false,e.getMessage());
        }catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"注册失败");
        }
    }
}
