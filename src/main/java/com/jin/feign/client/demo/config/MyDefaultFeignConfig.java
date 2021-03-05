package com.jin.feign.client.demo.config;

import com.jin.feign.client.demo.interceptor.MyRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wu.jinqing
 * @date 2021年02月19日
 */
@Configuration
public class MyDefaultFeignConfig {
    @Bean
   public RequestInterceptor myRequestInterceptor()
   {
       return new MyRequestInterceptor();
   }
}
