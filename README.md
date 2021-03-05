### feign

### 被@FeignClient注解修饰的类必须是接口，不然无法使用JDK的动态代理

### @EnableFeignClients 在springboot启动类中加入这个注解
> 加上@EnableFeignClients注解，会自动加入FeignClientsRegistrar，会扫描FeignClient注解, 并注册FeignClientFactoryBean、FeignClientSpecification对象。

```
@Import(FeignClientsRegistrar.class)
public @interface EnableFeignClients {
}

```
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
> org.springframework.cloud.openfeign.FeignClientsConfiguration
> 
> org.springframework.cloud.openfeign.FeignClientProperties

```
@ConfigurationProperties("feign.client")
public class FeignClientProperties {

	private boolean defaultToProperties = true;

	private String defaultConfig = "default";

	private Map<String, FeignClientConfiguration> config = new HashMap<>();
}

public static class FeignClientConfiguration {

		private Logger.Level loggerLevel;

		private Integer connectTimeout;

		private Integer readTimeout;

		private Class<Retryer> retryer;

		private Class<ErrorDecoder> errorDecoder;

		private List<Class<RequestInterceptor>> requestInterceptors;

		private Boolean decode404;

		private Class<Decoder> decoder;

		private Class<Encoder> encoder;

		private Class<Contract> contract;
}
```

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

### feign请求默认超时时间, connectTimeoutMillis默认10秒，readTimeoutMillis默认60秒
> feign.Request.Options

```
public static class Options {

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final boolean followRedirects;

    public Options(int connectTimeoutMillis, int readTimeoutMillis, boolean followRedirects) {
      this.connectTimeoutMillis = connectTimeoutMillis;
      this.readTimeoutMillis = readTimeoutMillis;
      this.followRedirects = followRedirects;
    }

    public Options(int connectTimeoutMillis, int readTimeoutMillis) {
      this(connectTimeoutMillis, readTimeoutMillis, true);
    }

    public Options() {
      this(10 * 1000, 60 * 1000);
    }

    /**
     * Defaults to 10 seconds. {@code 0} implies no timeout.
     *
     * @see java.net.HttpURLConnection#getConnectTimeout()
     */
    public int connectTimeoutMillis() {
      return connectTimeoutMillis;
    }

    /**
     * Defaults to 60 seconds. {@code 0} implies no timeout.
     *
     * @see java.net.HttpURLConnection#getReadTimeout()
     */
    public int readTimeoutMillis() {
      return readTimeoutMillis;
    }


    /**
     * Defaults to true. {@code false} tells the client to not follow the redirections.
     *
     * @see HttpURLConnection#getFollowRedirects()
     */
    public boolean isFollowRedirects() {
      return followRedirects;
    }
  }
```

### @FeignClient注解里面的url属性是否有值会影响Client的设置，最终影响执行的http请求，

#### url有值
> 默认使用feign.Client.Default，他采用最简单的HttpURLConnection完成网络请求。

```
<T> T getTarget() {
		FeignContext context = this.applicationContext.getBean(FeignContext.class);
		Feign.Builder builder = feign(context);

		if (!StringUtils.hasText(this.url)) {
			if (!this.name.startsWith("http")) {
				this.url = "http://" + this.name;
			}
			else {
				this.url = this.name;
			}
			this.url += cleanPath();
			return (T) loadBalance(builder, context,
					new HardCodedTarget<>(this.type, this.name, this.url));
		}
		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}
		String url = this.url + cleanPath();
		Client client = getOptional(context, Client.class);
		if (client != null) {
			if (client instanceof LoadBalancerFeignClient) {
				// not load balancing because we have a url,
				// but ribbon is on the classpath, so unwrap
				// 当url有值时使用的是委托client，即默认的feign.Client.Default
				client = ((LoadBalancerFeignClient) client).getDelegate();
			}
			builder.client(client);
		}
		Targeter targeter = get(context, Targeter.class);
		return (T) targeter.target(this, builder, context,
				new HardCodedTarget<>(this.type, this.name, url));
	}
```

#### url没有值
> 当url没有值，直接使用负载均衡Client

```
protected <T> T loadBalance(Feign.Builder builder, FeignContext context,
			HardCodedTarget<T> target) {
		Client client = getOptional(context, Client.class);
		if (client != null) {
		    // 当url没有值，直接使用负载均衡Client
			builder.client(client);
			Targeter targeter = get(context, Targeter.class);
			return targeter.target(this, builder, context, target);
		}

		throw new IllegalStateException(
				"No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-netflix-ribbon?");
	}
```


### feign.Request.Options 超时时间设置
> FeignClientFactoryBean.getTarget() -> FeignClientFactoryBean.feign() -> FeignClientFactoryBean.configureFeign() -> FeignClientFactoryBean.configureUsingConfiguration()或FeignClientFactoryBean.configureUsingProperties()

> FeignClientFactoryBean.configureUsingConfiguration()从bean容器获取。

> FeignClientFactoryBean.configureUsingProperties()从默认的配置及特定FeignClient的配置获取。

```
<T> T getTarget() {
		FeignContext context = this.applicationContext.getBean(FeignContext.class);
		Feign.Builder builder = feign(context);

		if (!StringUtils.hasText(this.url)) {
			if (!this.name.startsWith("http")) {
				this.url = "http://" + this.name;
			}
			else {
				this.url = this.name;
			}
			this.url += cleanPath();
			return (T) loadBalance(builder, context,
					new HardCodedTarget<>(this.type, this.name, this.url));
		}
		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}
		String url = this.url + cleanPath();
		Client client = getOptional(context, Client.class);
		if (client != null) {
			if (client instanceof LoadBalancerFeignClient) {
				// not load balancing because we have a url,
				// but ribbon is on the classpath, so unwrap
				client = ((LoadBalancerFeignClient) client).getDelegate();
			}
			builder.client(client);
		}
		Targeter targeter = get(context, Targeter.class);
		return (T) targeter.target(this, builder, context,
				new HardCodedTarget<>(this.type, this.name, url));
	}
	

protected Feign.Builder feign(FeignContext context) {
		FeignLoggerFactory loggerFactory = get(context, FeignLoggerFactory.class);
		Logger logger = loggerFactory.create(this.type);

		// @formatter:off
		Feign.Builder builder = get(context, Feign.Builder.class)
				// required values
				.logger(logger)
				.encoder(get(context, Encoder.class))
				.decoder(get(context, Decoder.class))
				.contract(get(context, Contract.class));
		// @formatter:on

		configureFeign(context, builder);

		return builder;
	}


protected void configureFeign(FeignContext context, Feign.Builder builder) {
		FeignClientProperties properties = this.applicationContext
				.getBean(FeignClientProperties.class);
		if (properties != null) {
			if (properties.isDefaultToProperties()) {
				configureUsingConfiguration(context, builder);
				configureUsingProperties(
						properties.getConfig().get(properties.getDefaultConfig()),
						builder);
				configureUsingProperties(properties.getConfig().get(this.contextId),
						builder);
			}
			else {
				configureUsingProperties(
						properties.getConfig().get(properties.getDefaultConfig()),
						builder);
				configureUsingProperties(properties.getConfig().get(this.contextId),
						builder);
				configureUsingConfiguration(context, builder);
			}
		}
		else {
			configureUsingConfiguration(context, builder);
		}
	}
	
protected void configureUsingConfiguration(FeignContext context,
			Feign.Builder builder) {
		Logger.Level level = getOptional(context, Logger.Level.class);
		if (level != null) {
			builder.logLevel(level);
		}
		Retryer retryer = getOptional(context, Retryer.class);
		if (retryer != null) {
			builder.retryer(retryer);
		}
		ErrorDecoder errorDecoder = getOptional(context, ErrorDecoder.class);
		if (errorDecoder != null) {
			builder.errorDecoder(errorDecoder);
		}
		Request.Options options = getOptional(context, Request.Options.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, RequestInterceptor> requestInterceptors = context
				.getInstances(this.contextId, RequestInterceptor.class);
		if (requestInterceptors != null) {
			builder.requestInterceptors(requestInterceptors.values());
		}

		if (this.decode404) {
			builder.decode404();
		}
	}
	
protected void configureUsingProperties(
			FeignClientProperties.FeignClientConfiguration config,
			Feign.Builder builder) {
		if (config == null) {
			return;
		}

		if (config.getLoggerLevel() != null) {
			builder.logLevel(config.getLoggerLevel());
		}

		if (config.getConnectTimeout() != null && config.getReadTimeout() != null) {
			builder.options(new Request.Options(config.getConnectTimeout(),
					config.getReadTimeout()));
		}

		if (config.getRetryer() != null) {
			Retryer retryer = getOrInstantiate(config.getRetryer());
			builder.retryer(retryer);
		}

		if (config.getErrorDecoder() != null) {
			ErrorDecoder errorDecoder = getOrInstantiate(config.getErrorDecoder());
			builder.errorDecoder(errorDecoder);
		}

		if (config.getRequestInterceptors() != null
				&& !config.getRequestInterceptors().isEmpty()) {
			// this will add request interceptor to builder, not replace existing
			for (Class<RequestInterceptor> bean : config.getRequestInterceptors()) {
				RequestInterceptor interceptor = getOrInstantiate(bean);
				builder.requestInterceptor(interceptor);
			}
		}

		if (config.getDecode404() != null) {
			if (config.getDecode404()) {
				builder.decode404();
			}
		}

		if (Objects.nonNull(config.getEncoder())) {
			builder.encoder(getOrInstantiate(config.getEncoder()));
		}

		if (Objects.nonNull(config.getDecoder())) {
			builder.decoder(getOrInstantiate(config.getDecoder()));
		}

		if (Objects.nonNull(config.getContract())) {
			builder.contract(getOrInstantiate(config.getContract()));
		}
	}	
		
			
```




















