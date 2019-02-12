package cn.itcast.core.service.user;

import cn.itcast.core.dao.user.UserDao;
import cn.itcast.core.pojo.user.User;
import cn.itcast.core.utils.md5.MD5Util;
import com.alibaba.dubbo.config.annotation.Service;


import org.apache.commons.lang.RandomStringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.annotation.Resource;
import javax.jms.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private JmsTemplate jmsTemplate;

    @Resource
    private Destination smsDestination;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Resource
    private UserDao userDao;


  //    @Resource
 //    private ActiveMQQueue AAAA;
    /**
     * 获取短信验证码
     * @param phone
     */

    @Override
    public void sendCode(final String phone) {

        //Destination activeMQQueue=new ActiveMQQueue("aaa");
        // 随机生成6位数的短信验证码
        final String code = RandomStringUtils.randomNumeric(6);
        System.out.println(code);
        //将验证码储存
        redisTemplate.boundValueOps(phone).set(code);
        //设置改验证码的过期时间
        redisTemplate.boundValueOps(phone).expire(5, TimeUnit.MINUTES);//这个是五分钟
        jmsTemplate.send(smsDestination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                // 发送的数据：手机号、验证码、签名、模板
                MapMessage mapMessage = session.createMapMessage();
                mapMessage.setString("phoneNumbers", phone);
                mapMessage.setString("signName", "阮文");
                mapMessage.setString("templateCode", "SMS_140720901");//格式
                mapMessage.setString("templateParam", "{\"code\":\""+code+"\"}");
                return mapMessage;
            }
        });
    }

    @Override
    public void add(String smscode, User user) {
        //首先验证 验证码是否正确
        String  code = (String) redisTemplate.boundValueOps(user.getPhone()).get();//这个是当你获取验证码时候己经把你的手机号和验证码做成map key就是手机号 vuils就是 验证码 :现在就是保存时候我要拿到验证码跟我rdsis数据库一样的才行
       if (code!=null&&smscode!=null&&!"".equals(smscode)&&code.equals(smscode)){
           //获取密码加密
           String password = user.getPassword();
           String s = MD5Util.MD5Encode(password, "");//""这是是否制定编码格式
           user.setPassword(s);
           user.setCreated(new Date());
           user.setUpdated(new Date());
           userDao.insert(user);
       }else {
           throw new RuntimeException("验证码不正确");
       }


    }
}
