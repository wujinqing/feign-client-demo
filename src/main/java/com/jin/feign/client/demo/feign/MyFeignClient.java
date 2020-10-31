package com.jin.feign.client.demo.feign;

import com.jin.feign.client.demo.bean.User;
import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author wu.jinqing
 * @date 2020年01月13日
 */
@FeignClient(name="MyFeignClient", url = "http://localhost:8080")
@Component
public interface MyFeignClient {
    @RequestMapping("/getUser?id={id}")
    public User getUser(@PathVariable("id") Long id);
}
