package cn.itcast.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 短信平台的启动器
 */
@SpringBootApplication
public class SmsApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SmsApplication.class);
        springApplication.run(args);
    }
}
