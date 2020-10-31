package com.jin.feign.client.demo;

import com.jin.feign.client.demo.bean.User;
import com.jin.feign.client.demo.feign.MyFeignClient;
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
@EnableFeignClients
public class Bootstrap {
    @Autowired
    private MyFeignClient myFeignClient;

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

    @RequestMapping("/getUser")
    public User getUser(Long id)
    {
        User u = myFeignClient.getUser(id);

        return u;
    }

}
