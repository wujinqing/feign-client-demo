## ribbon
> 配置项：com.netflix.client.config.CommonClientConfigKey

## 配置项
|配置项|说明|
|---|---|
|MaxAutoRetries|同一个服务的最大重试次数，默认是0|
|MaxAutoRetriesNextServer|从服务器列表中选择其他服务重试的次数， 默认是1，只会进行一次选择其他服务重试|
|||
|||
|||
## 负载均衡组件

### Rule 选择服务器的规则
> Rule - a logic component to determine which server to return from a list

### Ping 服务器心跳检测
> Ping - a component running in background to ensure liveness of servers

### ServerList 服务器列表
> ServerList - this can be static or dynamic. If it is dynamic (as used by DynamicServerListLoadBalancer), a background thread will refresh and filter the list at certain interval


## 负载均衡组件相关配置
|配置项|说明|
|---|---|
|\<clientName>.\<nameSpace>.NFLoadBalancerClassName|指定负载均衡类|
|\<clientName>.\<nameSpace>.NFLoadBalancerRuleClassName|指定选择服务器的规则的类|
|\<clientName>.\<nameSpace>.NFLoadBalancerPingClassName|指定服务器心跳检测的类|
|\<clientName>.\<nameSpace>.NIWSServerListClassName|指定获取服务器列表的类|
|\<clientName>.\<nameSpace>.NIWSServerListFilterClassName|指定服务器列表的过滤类|

### Common rules 常见的选择服务器的规则

#### RoundRobinRule
> 简单的轮询算法。

#### AvailabilityFilteringRule
> 这个规则会跳过触发熔断或者高并发连接数的服务，当一个服务在最近的3次连接中都失败时会触发熔断，熔断状态会保持30秒, 当一个服务连续触发熔断时，熔断状态持续时间会指数级增长。

```
# 到达多少次触发熔断 successive connection failures threshold to put the server in circuit tripped state, default 3
niws.loadbalancer.<clientName>.connectionFailureCountThreshold

# 熔断状态会持续时间 Maximal period that an instance can remain in "unusable" state regardless of the exponential increase, default 30
niws.loadbalancer.<clientName>.circuitTripMaxTimeoutSeconds

# 一个服务的最大连接数 threshold of concurrent connections count to skip the server, default is Integer.MAX_INT
<clientName>.<clientConfigNameSpace>.ActiveConnectionsLimit
```

#### WeightedResponseTimeRule
> 平均响应时间权重算法，平均响应时间越长权重越低.
 
```
<clientName>.<clientConfigNameSpace>.NFLoadBalancerRuleClassName=com.netflix.loadbalancer.WeightedResponseTimeRule

```

### ServerList 服务器列表

#### 1.通过API来设置服务器列表
```
BaseLoadBalancer.setServersList()
```

#### 2.通过配置文件的方式指定服务器列表
> 这种方式底层实现类是：ConfigurationBasedServerList
```
sample-client.ribbon.listOfServers=www.microsoft.com:80,www.yahoo.com:80,www.google.com:80
```

#### 3.通过服务发现的方式指定服务器列表，如: Eureka
> 这种方式底层实现类是：DiscoveryEnabledNIWSServerList

```
myClient.ribbon.NIWSServerListClassName=com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList 
# the server must register itself with Eureka server with VipAddress "myservice"
myClient.ribbon.DeploymentContextBasedVipAddresses=myservice
```

### ServerListFilter 服务器列表过滤器
> ServerListFilter是被DynamicServerListLoadBalancer使用的一个组件，用来从ServerList中过滤出需要的服务。
 
#### 1.ZoneAffinityServerListFilter
> 过滤出相同Zone的服务器列表，当在这个Zone中没有一个服务可用时会现在其他Zone的服务。
> 
```
myclient.ribbon.EnableZoneAffinity=true
```
#### 2.ServerListSubsetFilter
> 只会返回ServerList服务器列表的子集，当子集里面的服务的可用性差时，会定期的从ServerList的其他服务中替换掉子集中部分服务。


```
myClient.ribbon.NIWSServerListClassName=com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList 
# the server must register itself with Eureka server with VipAddress "myservice"
myClient.ribbon.DeploymentContextBasedVipAddresses=myservice
myClient.ribbon.NIWSServerListFilterClassName=com.netflix.loadbalancer.ServerListSubsetFilter
# only show client 5 servers. default is 20.
myClient.ribbon.ServerListSubsetFilter.size=5
```
### 配置示例

```
# Max number of retries on the same server (excluding the first try)
sample-client.ribbon.MaxAutoRetries=1

# Max number of next servers to retry (excluding the first server)
sample-client.ribbon.MaxAutoRetriesNextServer=1

# Whether all operations can be retried for this client
sample-client.ribbon.OkToRetryOnAllOperations=true

# Interval to refresh the server list from the source
sample-client.ribbon.ServerListRefreshInterval=2000

# Connect timeout used by Apache HttpClient
sample-client.ribbon.ConnectTimeout=3000

# Read timeout used by Apache HttpClient
sample-client.ribbon.ReadTimeout=3000

# Initial list of servers, can be changed via Archaius dynamic property at runtime
sample-client.ribbon.listOfServers=www.microsoft.com:80,www.yahoo.com:80,www.google.com:80
```

### \<clientName>.\<nameSpace>.\<propertyName>=\<value>中的\<nameSpace>的作用
> 用来指定初始化IClientConfig时使用哪个命名空间的配置，默认值是ribbon。
 
```
public static final String DEFAULT_PROPERTY_NAME_SPACE = "ribbon";

private String propertyNameSpace = DEFAULT_PROPERTY_NAME_SPACE;
    
public DefaultClientConfigImpl() {
        this.dynamicProperties.clear();
        this.enableDynamicProperties = false;
    }

	/**
	 * Create instance with no properties in the specified name space
	 */
    public DefaultClientConfigImpl(String nameSpace) {
    	this();
    	this.propertyNameSpace = nameSpace;
    }
```

#### 示例: 指定命名空间 "foo"

```
DefaultClientConfigImpl clientConfig = new DefaultClientConfigImpl("foo");
  clientConfig.loadProperites("myclient");
  MyClient client = (MyClient) ClientFactory.registerClientFromProperties("myclient", clientConfig);
```







