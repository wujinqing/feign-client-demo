package com.jin.feign.client.demo;

import com.jin.feign.client.demo.bean.User;
import com.jin.feign.client.demo.config.MyDefaultFeignConfig;
import com.jin.feign.client.demo.feign.MyFeignClient;
import com.jin.feign.client.demo.feign.MyFeignClient2;
import feign.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wu.jinqing
 * @date 2020年01月13日
 */
@SpringBootApplication
@RestController
@EnableFeignClients(defaultConfiguration = {MyDefaultFeignConfig.class})
public class Bootstrap {
    @Autowired
    private MyFeignClient myFeignClient;
    @Autowired
    private MyFeignClient2 myFeignClient2;

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

    @RequestMapping("/getUser")
    public User getUser(Long id)
    {
        User u = myFeignClient.getUser(id);

        return u;
    }

    @RequestMapping("/timeout")
    public User timeout(Long timeout)
    {
        long st = System.currentTimeMillis();
        System.out.println("开始：" + st);
        User u = new User();
        try {
            u = myFeignClient2.timeout(timeout);
        }catch (Exception e)
        {

        }

        u.setCost("接口耗时：【" + (System.currentTimeMillis() - st) + "】毫秒");
        System.out.println("耗时：" + (System.currentTimeMillis() - st));
        return u;
    }

    @RequestMapping("/getUser2")
    public User getUser2(Long id)
    {
        User u = myFeignClient2.getUser2(id);

        return u;
    }

}
