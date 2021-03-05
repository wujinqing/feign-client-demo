## Feign超时时间设置
> 当设置了feign自己的超时时间时，不会使用ribbon的超时时间。

## 超时时间计算
> 只是连接超时：(MaxAutoRetriesNextServer + 1)*(MaxAutoRetries + 1)*connectTimeout

> 只是读超时：(MaxAutoRetriesNextServer + 1)*(MaxAutoRetries + 1)*readTimeout

> 连接和读都超时：(MaxAutoRetriesNextServer + 1)*(MaxAutoRetries + 1)*connectTimeout + (MaxAutoRetriesNextServer + 1)*(MaxAutoRetries + 1)*readTimeout

### 测试数据

|connectTimeout|readTimeout|MaxAutoRetries|MaxAutoRetriesNextServer|接口读超时|接口连接超时|
|---|---|---|---|---|---|
|1秒|3秒|0次|1次|6秒((3秒 + 0次*3秒) + 1次*(3秒 + 0次*3秒))|2秒((1秒 + 0次*1秒) + 1次*(1秒 + 0次*1秒))|
|1秒|3秒|1次|1次|12秒((3秒 + 1次*3秒) + 1次*(3秒 + 1次*3秒))|4秒((1秒 + 1次*1秒) + 1次*(1秒 + 1次*1秒))|
|1秒|3秒|2次|1次|18秒((3秒 + 2次*3秒) + 1次*(3秒 + 2次*3秒))|6秒((1秒 + 2次*1秒) + 1次*(1秒 + 2次*1秒))|
|1秒|3秒|2次|2次|27秒((3秒 + 2次*3秒) + 2次*(3秒 + 2次*3秒))|9秒((1秒 + 2次*1秒) + 2次*(1秒 + 2次*1秒))|
|1秒|3秒|3次|2次|36秒((3秒 + 3次*3秒) + 2次*(3秒 + 3次*3秒))|12秒((1秒 + 3次*1秒) + 2次*(1秒 + 3次*1秒))|

### MaxAutoRetries：同一台机器的重试次数
### MaxAutoRetriesNextServer：找几台其他机器重试

### 配置超时时间的方式, 优先级：3 > 2 > 1 > 5 > 4

#### 1.通过代码的方式

```
    @Bean
	@ConditionalOnMissingBean
	public Request.Options feignRequestOptions() {
		return LoadBalancerFeignClient.DEFAULT_OPTIONS;
	}
```

### 2.通过配置文件设置默认超时时间

```
feign:
  client:
    config:
      default:
        connectTimeout: 50000
        readTimeout: 50000
```


### 3.通过配置文件设置指定FeignClient超时时间

```
feign:
  client:
    config:
      MyFeignClient2:
        connectTimeout: 60000
        readTimeout: 60000
```


### 4.通过配置文件设置ribbon的默认超时时间
```
ribbon:
  ReadTimeout: 1000
  ConnectTimeout: 1000
```

### 5.通过配置文件设置ribbon的指定FeignClient超时时间
```
MyFeignClient2:
  ribbon:
    ReadTimeout: 2000
    ConnectTimeout: 2000
    MaxAutoRetries: 0
    MaxAutoRetriesNextServer: 1
```



### 当设置了feign自己的超时时间时，不会使用ribbon的超时时间。


```
org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer.execute()

@Override
	public RibbonResponse execute(RibbonRequest request, IClientConfig configOverride)
			throws IOException {
		Request.Options options;
		if (configOverride != null) {
			RibbonProperties override = RibbonProperties.from(configOverride);
			options = new Request.Options(override.connectTimeout(this.connectTimeout),
					override.readTimeout(this.readTimeout));
		}
		else {
			options = new Request.Options(this.connectTimeout, this.readTimeout);
		}
		Response response = request.client().execute(request.toRequest(), options);
		return new RibbonResponse(request.getUri(), response);
	}
```







