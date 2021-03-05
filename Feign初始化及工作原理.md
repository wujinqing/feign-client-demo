## Feign初始化及工作原理

> 1.通过JDK的动态代理生成MyFeignClient的代理对象，实际调用类型是FeignInvocationHandler

> 2.当调用MyFeignClient里面的方法时，实际上是调用FeignInvocationHandler里面的invoke()方法。

> 3.invoke()方法通过Method对象在dispatch维护的Map中找到对应的MethodHandler。dispatch实际上是Map<String, MethodHandler> nameToHandler，方法和方法处理对象的映射。

> 4.将FeignInvocationHandler的invoke()方法的调用，委托给SynchronousMethodHandler的invoke()方法。

> 5.真实的底层网络请求是在SynchronousMethodHandler的invoke()方法中完成的。

## Feign初始化流程

> 1.在启动类上加上@EnableFeignClients注解。

> 2.在@EnableFeignClients注解里面通过@Import(FeignClientsRegistrar.class)导入FeignClientsRegistrar这个bean定义注册器。

> 3.在FeignClientsRegistrar类中会扫描所有被@FeignClient注解修饰的类，并把每一个自定义的被@FeignClient注解修饰的接口封装成一个对应的FeignClientFactoryBean实例的bean定义。

> 4.通过FeignClientFactoryBean的getObject()方法生成对应的自定义的MyFeignClient代理实例。

## FeignClientFactoryBean生成自定义FeignClient代理实例流程

> 1.扫描所有被@FeignClient注解修饰的类或者接口，将对应的类或者接口的信息以及@FeignClient注解的信息封装成一个FeignClientFactoryBean对象。

> 2.通过getObject()方法生成对应的自定义的MyFeignClient代理实例。

## FeignClientFactoryBean的getObject()方法执行逻辑剖析
> 1.通过applicationContext获取FeignAutoConfiguration里面自动配置的FeignContext上下文对象。

> 2.为当前的FeignClient创建一个独立上下文对象(AnnotationConfigApplicationContext)。

> 3.将@FeignClient注解里面的configuration(数组)配置里面的class对象里面的配置注册到当前FeignClient上下文当中。

> 4.将@EnableFeignClients注解里面的defaultConfiguration(数组)配置里面的class对象里面的配置注册到当前FeignClient上下文当中。

> 5.将FeignClientsConfiguration对象里面的配置注册到当前FeignClient上下文当中。

> 6.从FeignClient上下文当中获取到Encoder.class，Decoder.class，Contract.class，FeignLoggerFactory.class实例存放在Feign.Builder中用来构建FeignClient代理实例。

> 7.使用FeignClient上下文的bean容器里面的类配置, 从@FeignClient注解里面的configuration(数组)配置，FeignClientsConfiguration对象里面的配置；来设置Feign.Builder。

> 8.使用FeignClientProperties指定的全局默认配置(名字为default的配置)；来设置Feign.Builder。

> 9.使用FeignClientProperties指定的特定于当前FeignClient配置；来设置Feign.Builder。

> 10.从FeignClient上下文的bean容器里面获取Targeter对象，调用它的target()方法生成FeignClient代理实例。

## Targeter生成FeignClient代理实例流程

> 1.Targeter的target()方法会调用Feign.Builder.target()。

> 2.调用build()方法生成ReflectiveFeign对象。
> 
> 3.调用newInstance()方法，使用JDK的动态代理生成FeignClient代理实例(被@FeignClient注解修饰的类必须是接口，不然无法使用JDK的动态代理)。

## newInstance()方法剖析
> 1.通过Contract解析自定义@FeignClient接口里面的每一个方法，解析成一个个SynchronousMethodHandler对象.

> 2.创建JDK动态代理的调用处理器实例，类型是ReflectiveFeign.FeignInvocationHandler。

> 3.通过JDK动态代理使用自定义@FeignClient注解修饰的接口和和上面创建的InvocationHandler创建代理实例并返回(即实现了MyFeignClient接口的类，我们通过spring容器自动注入的MyFeignClient对象就是这个代理类)。


### RequestInterceptor拦截器执行
> 拦截器在这个方法中执行feign.SynchronousMethodHandler.targetRequest()。

```
Request targetRequest(RequestTemplate template) {
    for (RequestInterceptor interceptor : requestInterceptors) {
      interceptor.apply(template);
    }
    return target.apply(template);
  }
```
### 所有对MyFeignClient接口里面的方法的调用都会代理到ReflectiveFeign.FeignInvocationHandler.invoke()方法中执行。
```
@Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("equals".equals(method.getName())) {
        try {
          Object otherHandler =
              args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
          return equals(otherHandler);
        } catch (IllegalArgumentException e) {
          return false;
        }
      } else if ("hashCode".equals(method.getName())) {
        return hashCode();
      } else if ("toString".equals(method.getName())) {
        return toString();
      }

      return dispatch.get(method).invoke(args);
    }
```

### 所有对MyFeignClient接口里面的方法的调用最终会调用SynchronousMethodHandler.invoke()方法。
```

public Object invoke(Object[] argv) throws Throwable {
    RequestTemplate template = buildTemplateFromArgs.create(argv);
    Retryer retryer = this.retryer.clone();
    while (true) {
      try {
        return executeAndDecode(template);
      } catch (RetryableException e) {
        try {
          retryer.continueOrPropagate(e);
        } catch (RetryableException th) {
          Throwable cause = th.getCause();
          if (propagationPolicy == UNWRAP && cause != null) {
            throw cause;
          } else {
            throw th;
          }
        }
        if (logLevel != Logger.Level.NONE) {
          logger.logRetry(metadata.configKey(), logLevel);
        }
        continue;
      }
    }
  }
```
### 通过JDK动态代理生成代理类
```
public <T> T newInstance(Target<T> target) {
    // key:MyFeignClient#timeout(Long), value: SynchronousMethodHandler
    Map<String, MethodHandler> nameToHandler = targetToHandlersByName.apply(target);
    Map<Method, MethodHandler> methodToHandler = new LinkedHashMap<Method, MethodHandler>();
    List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<DefaultMethodHandler>();

    for (Method method : target.type().getMethods()) {
      if (method.getDeclaringClass() == Object.class) {
        continue;
      } else if (Util.isDefault(method)) {
        DefaultMethodHandler handler = new DefaultMethodHandler(method);
        defaultMethodHandlers.add(handler);
        methodToHandler.put(method, handler);
      } else {
        methodToHandler.put(method, nameToHandler.get(Feign.configKey(target.type(), method)));
      }
    }
    // 创建调用处理器
    InvocationHandler handler = factory.create(target, methodToHandler);
    // 通过JDK动态代理生成代理类
    T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(),
        new Class<?>[] {target.type()}, handler);

    for (DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
      defaultMethodHandler.bindTo(proxy);
    }
    return proxy;
  }
```

### 生成ReflectiveFeign对象
```
public <T> T target(Target<T> target) {
      return build().newInstance(target);
    }

    public Feign build() {
      SynchronousMethodHandler.Factory synchronousMethodHandlerFactory =
          new SynchronousMethodHandler.Factory(client, retryer, requestInterceptors, logger,
              logLevel, decode404, closeAfterDecode, propagationPolicy);
      ParseHandlersByName handlersByName =
          new ParseHandlersByName(contract, options, encoder, decoder, queryMapEncoder,
              errorDecoder, synchronousMethodHandlerFactory);
      return new ReflectiveFeign(handlersByName, invocationHandlerFactory, queryMapEncoder);
    }
```
### 从三个地方配置Feign.Builder

```
protected void configureFeign(FeignContext context, Feign.Builder builder) {
		FeignClientProperties properties = this.applicationContext
				.getBean(FeignClientProperties.class);
		if (properties != null) {
			if (properties.isDefaultToProperties()) {
			   // 使用FeignClient上下文的bean容器里面的类配置, 从@FeignClient注解里面的configuration(数组)配置，FeignClientsConfiguration对象里面的配置。
				configureUsingConfiguration(context, builder);
				// 使用FeignClientProperties指定的全局默认配置(名字为default的配置)
				configureUsingProperties(
						properties.getConfig().get(properties.getDefaultConfig()),
						builder);
				// 使用FeignClientProperties指定的特定于当前FeignClient配置		
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
	
	
	protected void configureUsingProperties(
			FeignClientProperties.FeignClientConfiguration config,
			Feign.Builder builder) {
		if (config == null) {
			return;
		}

		if (config.getLoggerLevel() != null) {
			builder.logLevel(config.getLoggerLevel());
		}

       // ConnectTimeout和ReadTimeout要同时设置才会生效
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
### 从FeignClient上下文当中获取到Encoder.class，Decoder.class，Contract.class，FeignLoggerFactory.class实例存放在Feign.Builder中用来构建FeignClient代理实例。

```
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
```

### 为每一个FeignClient创建一个独立上下文对象
``` 
protected AnnotationConfigApplicationContext getContext(String name) {
		if (!this.contexts.containsKey(name)) {
			synchronized (this.contexts) {
				if (!this.contexts.containsKey(name)) {
					this.contexts.put(name, createContext(name));
				}
			}
		}
		return this.contexts.get(name);
	}

	protected AnnotationConfigApplicationContext createContext(String name) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (this.configurations.containsKey(name)) {
		   // 将@FeignClient注解里面的configuration(数组)配置里面的class对象里面的配置注册到当前FeignClient上下文当中。
			for (Class<?> configuration : this.configurations.get(name)
					.getConfiguration()) {
				context.register(configuration);
			}
		}
		
		
		for (Map.Entry<String, C> entry : this.configurations.entrySet()) {
		
		    // 将@EnableFeignClients注解里面的defaultConfiguration(数组)配置里面的class对象里面的配置注册到当前FeignClient上下文当中。
			if (entry.getKey().startsWith("default.")) {
				for (Class<?> configuration : entry.getValue().getConfiguration()) {
					context.register(configuration);
				}
			}
		}
		
		// 将FeignClientsConfiguration对象里面的配置注册到当前FeignClient上下文当中。
		context.register(PropertyPlaceholderAutoConfiguration.class,
				this.defaultConfigType);
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
				this.propertySourceName,
				Collections.<String, Object>singletonMap(this.propertyName, name)));
		if (this.parent != null) {
			// Uses Environment from parent as well as beans
			context.setParent(this.parent);
			// jdk11 issue
			// https://github.com/spring-cloud/spring-cloud-netflix/issues/3101
			context.setClassLoader(this.parent.getClassLoader());
		}
		context.setDisplayName(generateDisplayName(name));
		context.refresh();
		return context;
	}

```
## MyFeignClient的代理对象初始化过程

### 通过org.springframework.cloud.openfeign.FeignClientFactoryBean的getTarget()构建Targeter
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

```

### 调用feign.Feign.Builder.target()方法
```
public <T> T target(Target<T> target) {
      return build().newInstance(target);
    }
```

### 通过feign.Feign.Builder的build()构建ReflectiveFeign对象
``` 
public Feign build() {
      SynchronousMethodHandler.Factory synchronousMethodHandlerFactory =
          new SynchronousMethodHandler.Factory(client, retryer, requestInterceptors, logger,
              logLevel, decode404, closeAfterDecode, propagationPolicy);
      ParseHandlersByName handlersByName =
          new ParseHandlersByName(contract, options, encoder, decoder, queryMapEncoder,
              errorDecoder, synchronousMethodHandlerFactory);
      return new ReflectiveFeign(handlersByName, invocationHandlerFactory, queryMapEncoder);
    }
```


### 通过Proxy.newProxyInstance()  JDK的动态代理构建实现了我们自定义的接口MyFeignClient的代理对象，实际的调用对象是FeignInvocationHandler

``` 
public <T> T newInstance(Target<T> target) {
    // 解析MyFeignClient的各个方法，targetToHandlersByName=feign.ReflectiveFeign.ParseHandlersByName
    Map<String, MethodHandler> nameToHandler = targetToHandlersByName.apply(target);
    Map<Method, MethodHandler> methodToHandler = new LinkedHashMap<Method, MethodHandler>();
    List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<DefaultMethodHandler>();

    for (Method method : target.type().getMethods()) {
      if (method.getDeclaringClass() == Object.class) {
        continue;
      } else if (Util.isDefault(method)) {
        DefaultMethodHandler handler = new DefaultMethodHandler(method);
        defaultMethodHandlers.add(handler);
        methodToHandler.put(method, handler);
      } else {
        methodToHandler.put(method, nameToHandler.get(Feign.configKey(target.type(), method)));
      }
    }
    // 真实的调用者是feign.ReflectiveFeign.FeignInvocationHandler
    InvocationHandler handler = factory.create(target, methodToHandler);
    T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(),
        new Class<?>[] {target.type()}, handler);

    for (DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
      defaultMethodHandler.bindTo(proxy);
    }
    return proxy;
  }
```



### 解析MyFeignClient的各个方法，具体是在这行代码实现的Map<String, MethodHandler> nameToHandler = targetToHandlersByName.apply(target);


#### feign.ReflectiveFeign.ParseHandlersByName.apply()方法
> 解析MyFeignClient接口及其里面方法的各种注解元数据主要是解析RequestMapping注解。

``` 
public Map<String, MethodHandler> apply(Target key) {
       // 解析MyFeignClient接口及其里面方法的各种注解元数据主要是解析RequestMapping。contract实际类型是SpringMvcContract
      List<MethodMetadata> metadata = contract.parseAndValidatateMetadata(key.type());
      Map<String, MethodHandler> result = new LinkedHashMap<String, MethodHandler>();
      for (MethodMetadata md : metadata) {
        BuildTemplateByResolvingArgs buildTemplate;
        if (!md.formParams().isEmpty() && md.template().bodyTemplate() == null) {
          buildTemplate = new BuildFormEncodedTemplateFromArgs(md, encoder, queryMapEncoder);
        } else if (md.bodyIndex() != null) {
          buildTemplate = new BuildEncodedTemplateFromArgs(md, encoder, queryMapEncoder);
        } else {
          buildTemplate = new BuildTemplateByResolvingArgs(md, queryMapEncoder);
        }
        result.put(md.configKey(),
            factory.create(key, md, buildTemplate, options, decoder, errorDecoder));
      }
      return result;
    }
```

#### 在SpringMvcContract的processAnnotationOnMethod方法中设置RequestTemplate相关信息
``` 
@Override
	protected void processAnnotationOnMethod(MethodMetadata data,
			Annotation methodAnnotation, Method method) {
		if (!RequestMapping.class.isInstance(methodAnnotation) && !methodAnnotation
				.annotationType().isAnnotationPresent(RequestMapping.class)) {
			return;
		}

		RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
		// HTTP Method
		RequestMethod[] methods = methodMapping.method();
		if (methods.length == 0) {
			methods = new RequestMethod[] { RequestMethod.GET };
		}
		checkOne(method, methods, "method");
		data.template().method(Request.HttpMethod.valueOf(methods[0].name()));

		// path
		checkAtMostOne(method, methodMapping.value(), "value");
		if (methodMapping.value().length > 0) {
			String pathValue = emptyToNull(methodMapping.value()[0]);
			if (pathValue != null) {
				pathValue = resolve(pathValue);
				// Append path from @RequestMapping if value is present on method
				if (!pathValue.startsWith("/") && !data.template().path().endsWith("/")) {
					pathValue = "/" + pathValue;
				}
				data.template().uri(pathValue, true);
			}
		}

		// produces
		parseProduces(data, method, methodMapping);

		// consumes
		parseConsumes(data, method, methodMapping);

		// headers
		parseHeaders(data, method, methodMapping);

		data.indexToExpander(new LinkedHashMap<Integer, Param.Expander>());
	}

```
### 通过feign.SynchronousMethodHandler.Factory创建MethodHandler，实际类型是SynchronousMethodHandler

``` 
public MethodHandler create(Target<?> target,
                                MethodMetadata md,
                                RequestTemplate.Factory buildTemplateFromArgs,
                                Options options,
                                Decoder decoder,
                                ErrorDecoder errorDecoder) {
      return new SynchronousMethodHandler(target, client, retryer, requestInterceptors, logger,
          logLevel, md, buildTemplateFromArgs, options, decoder,
          errorDecoder, decode404, closeAfterDecode, propagationPolicy);
    }
```


## com.jin.feign.client.demo.feign.MyFeignClient具体方法的调用过程

> 1.通过JDK的动态代理生成MyFeignClient的代理对象，实际调用类型是FeignInvocationHandler

> 2.当调用MyFeignClient里面的方法时，实际上是调用FeignInvocationHandler里面的invoke()方法。

> 3.invoke()方法通过Method对象在dispatch维护的Map中找到对应的MethodHandler。dispatch实际上是Map<String, MethodHandler> nameToHandler，方法和方法处理对象的映射。

> 4.将FeignInvocationHandler的invoke()方法的调用，委托给SynchronousMethodHandler的invoke()方法。

> 5.真实的底层网络请求是在SynchronousMethodHandler的invoke()方法中完成的。


#### FeignInvocationHandler的invoke()方法
```
@Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("equals".equals(method.getName())) {
        try {
          Object otherHandler =
              args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
          return equals(otherHandler);
        } catch (IllegalArgumentException e) {
          return false;
        }
      } else if ("hashCode".equals(method.getName())) {
        return hashCode();
      } else if ("toString".equals(method.getName())) {
        return toString();
      }

      // dispatch是Map<String, MethodHandler> nameToHandler。
      return dispatch.get(method).invoke(args);
    }
```


#### SynchronousMethodHandler的invoke()方法
> 发起网络请求response = client.execute(request, options);

```
@Override
  public Object invoke(Object[] argv) throws Throwable {
    RequestTemplate template = buildTemplateFromArgs.create(argv);
    Retryer retryer = this.retryer.clone();
    while (true) {
      try {
        return executeAndDecode(template);
      } catch (RetryableException e) {
        try {
          retryer.continueOrPropagate(e);
        } catch (RetryableException th) {
          Throwable cause = th.getCause();
          if (propagationPolicy == UNWRAP && cause != null) {
            throw cause;
          } else {
            throw th;
          }
        }
        if (logLevel != Logger.Level.NONE) {
          logger.logRetry(metadata.configKey(), logLevel);
        }
        continue;
      }
    }
  }

  Object executeAndDecode(RequestTemplate template) throws Throwable {
    Request request = targetRequest(template);

    if (logLevel != Logger.Level.NONE) {
      logger.logRequest(metadata.configKey(), logLevel, request);
    }

    Response response;
    long start = System.nanoTime();
    try {
        // 真正的网络请求，默认使用HttpURLConnection
      response = client.execute(request, options);
    } catch (IOException e) {
      if (logLevel != Logger.Level.NONE) {
        logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime(start));
      }
      throw errorExecuting(request, e);
    }
    long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    boolean shouldClose = true;
    try {
      if (logLevel != Logger.Level.NONE) {
        response =
            logger.logAndRebufferResponse(metadata.configKey(), logLevel, response, elapsedTime);
      }
      if (Response.class == metadata.returnType()) {
        if (response.body() == null) {
          return response;
        }
        if (response.body().length() == null ||
            response.body().length() > MAX_RESPONSE_BUFFER_SIZE) {
          shouldClose = false;
          return response;
        }
        // Ensure the response body is disconnected
        byte[] bodyData = Util.toByteArray(response.body().asInputStream());
        return response.toBuilder().body(bodyData).build();
      }
      if (response.status() >= 200 && response.status() < 300) {
        if (void.class == metadata.returnType()) {
          return null;
        } else {
           // 处理响应结果
          Object result = decode(response);
          shouldClose = closeAfterDecode;
          return result;
        }
      } else if (decode404 && response.status() == 404 && void.class != metadata.returnType()) {
        Object result = decode(response);
        shouldClose = closeAfterDecode;
        return result;
      } else {
        throw errorDecoder.decode(metadata.configKey(), response);
      }
    } catch (IOException e) {
      if (logLevel != Logger.Level.NONE) {
        logger.logIOException(metadata.configKey(), logLevel, e, elapsedTime);
      }
      throw errorReading(request, response, e);
    } finally {
      if (shouldClose) {
        ensureClosed(response.body());
      }
    }
  }

```

### 通过SpringDecoder的messageConverters的MappingJackson2HttpMessageConverter处理响应结果

``` 
Object decode(Response response) throws Throwable {
    try {
      return decoder.decode(response, metadata.returnType());
    } catch (FeignException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new DecodeException(response.status(), e.getMessage(), e);
    }
  }
  
  
@Override
	public Object decode(final Response response, Type type)
			throws IOException, FeignException {
		if (type instanceof Class || type instanceof ParameterizedType
				|| type instanceof WildcardType) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor(
					type, this.messageConverters.getObject().getConverters());

			return extractor.extractData(new FeignResponseAdapter(response));
		}
		throw new DecodeException(response.status(),
				"type is not an instance of Class or ParameterizedType: " + type);
	}
```


### 被@FeignClient注解修饰的类必须是接口

```
org.springframework.cloud.openfeign.FeignClientsRegistrar.registerFeignClients

public void registerFeignClients(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		ClassPathScanningCandidateComponentProvider scanner = getScanner();
		scanner.setResourceLoader(this.resourceLoader);

		Set<String> basePackages;

		Map<String, Object> attrs = metadata
				.getAnnotationAttributes(EnableFeignClients.class.getName());
		AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(
				FeignClient.class);
		final Class<?>[] clients = attrs == null ? null
				: (Class<?>[]) attrs.get("clients");
		if (clients == null || clients.length == 0) {
			scanner.addIncludeFilter(annotationTypeFilter);
			basePackages = getBasePackages(metadata);
		}
		else {
			final Set<String> clientClasses = new HashSet<>();
			basePackages = new HashSet<>();
			for (Class<?> clazz : clients) {
				basePackages.add(ClassUtils.getPackageName(clazz));
				clientClasses.add(clazz.getCanonicalName());
			}
			AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
				@Override
				protected boolean match(ClassMetadata metadata) {
					String cleaned = metadata.getClassName().replaceAll("\\$", ".");
					return clientClasses.contains(cleaned);
				}
			};
			scanner.addIncludeFilter(
					new AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
		}

		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidateComponents = scanner
					.findCandidateComponents(basePackage);
			for (BeanDefinition candidateComponent : candidateComponents) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					// verify annotated class is an interface
					AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					// 验证被@FeignClient注解修饰的类必须是接口
					Assert.isTrue(annotationMetadata.isInterface(),
							"@FeignClient can only be specified on an interface");

					Map<String, Object> attributes = annotationMetadata
							.getAnnotationAttributes(
									FeignClient.class.getCanonicalName());

					String name = getClientName(attributes);
					registerClientConfiguration(registry, name,
							attributes.get("configuration"));

					registerFeignClient(registry, annotationMetadata, attributes);
				}
			}
		}
	}

```
















