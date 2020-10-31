### feign


## Feign工作原理

> 1.通过JDK的动态代理生成MyFeignClient的代理对象，实际调用类型是FeignInvocationHandler。

> 2.当调用MyFeignClient里面的方法时，实际上是调用FeignInvocationHandler里面的invoke()方法。

> 3.FeignInvocationHandler的invoke()方法通过Method对象在dispatch维护的Map中找到对应的MethodHandler。(dispatch实际上是Map<String, MethodHandler> nameToHandler，方法和方法处理对象的映射。)

> 4.将FeignInvocationHandler的invoke()方法的调用，委托给SynchronousMethodHandler的invoke()方法。

> 5.真实的底层网络请求是在SynchronousMethodHandler的invoke()方法中完成的(默认使用HttpURLConnection)。


### feign配置信息参考这个类org.springframework.cloud.openfeign.FeignClientProperties及org.springframework.cloud.openfeign.FeignClientProperties.FeignClientConfiguration


### 设置默认超时时间
> 对应的是这个类Request.Options

```
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: basic

设置某个FeignClient超时时间        
feign:
  client:
    config:
      MyFeignClient:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: basic
                        
```

### 默认配置在FeignClientsConfiguration类


### Feign request/response compression 请求/响应压缩

```
feign.compression.request.enabled=true
feign.compression.response.enabled=true


feign.compression.request.enabled=true
feign.compression.request.mime-types=text/xml,application/xml,application/json
feign.compression.request.min-request-size=2048




```


### Feign logging 日志

```
logging.level.project.user.UserClient: DEBUG
```



```

The Logger.Level object that you may configure per client, tells Feign how much to log. Choices are:

NONE, No logging (DEFAULT).

BASIC, Log only the request method and URL and the response status code and execution time.

HEADERS, Log the basic information along with request and response headers.

FULL, Log the headers, body, and metadata for both requests and responses.


@Configuration
public class FooConfiguration {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

```






























