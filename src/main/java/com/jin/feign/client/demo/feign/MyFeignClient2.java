package com.jin.feign.client.demo.feign;

import com.jin.feign.client.demo.bean.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author wu.jinqing
 * @date 2021年02月20日
 */
@FeignClient(name="MyFeignClient2")
@Component
public interface MyFeignClient2 {
    @RequestMapping("/getUser2?id={id}")
    User getUser2(@PathVariable("id") Long id);

    @RequestMapping("/timeout?timeout={timeout}")
    User timeout(@PathVariable("timeout")Long timeout);
}
