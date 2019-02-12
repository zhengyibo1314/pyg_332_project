package cn.itcast.core.controller.login;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    /**
     * 显示当前登录人
     * @return
     */
    @RequestMapping("/showName.do")
    public Map<String, String> showName(){
        // 当前登录人在springsecurity容器中
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 将数据放到map中
        Map<String, String> map = new HashMap<>();
        map.put("username", username);  //这个键必须为indexController一致
        return map;
    }
}
