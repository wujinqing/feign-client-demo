package com.jin.feign.client.demo.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * @author wu.jinqing
 * @date 2021年02月18日
 */
public class MyRequestInterceptor3 implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {

        System.out.println("MyRequestInterceptor3");
    }
}
