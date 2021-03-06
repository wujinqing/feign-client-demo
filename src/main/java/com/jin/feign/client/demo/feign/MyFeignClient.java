package com.jin.feign.client.demo.feign;

import com.jin.feign.client.demo.bean.User;
import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author wu.jinqing
 * @date 2020年01月13日
 */
@FeignClient(name="MyFeignClient", url = "http://localhost:8080")
@Component
public interface MyFeignClient {
    @RequestMapping("/getUser?id={id}")
    User getUser(@PathVariable("id") Long id);

    @RequestMapping("/timeout?timeout={timeout}")
    User timeout(@PathVariable("timeout")Long timeout);

}
