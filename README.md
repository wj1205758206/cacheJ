# cacheJ
此项目主要实现了分布式二级缓存功能，同时支持接口调用的鉴权和限流，可用于open API的建设
- 二级缓存
  1. mysql存储用户元数据信息
  2. caffeine作为一级缓存(本地缓存)
  3. redis作为二级缓存
  4. canal订阅mysql的binlog，利用kafka消息队列推送给redis，实现异步更新缓存
- 鉴权和限流
  1. 用户每次调用接口时，通过二级缓存中缓存的用户元数据信息进行鉴权
  2. 使用redisson的限流器，基于redis中间件实现分布式限流，针对用户申请token进行流量控制
  3. 限流服务支持降级，当redis宕机时，分布式限流降级为单机限流，保证限流可用性和数据库稳定性

# cacheJ整体架构设计
![image](https://user-images.githubusercontent.com/52147760/178099257-0992bdde-baa4-43c6-a245-e00702129551.png)

- 对于读操作，优先查询本地缓存，本地缓存失效，再尝试查询redis缓存，redis缓存如果也失效，最后查询mysql，同时将查询结果进行缓存。
- 对于写操作，包括添加、修改、删除等操作，需要写mysql数据库，使用canal中间件订阅mysql的binlog记录，这样一旦mysql中产生了insert/update/delete操作，就可以把binlog相关消息推送给kafka消息队列，通过监听消费kafka中的消息，根据binlog记录更新redis和caffeine中的缓存，实现异步更新缓存。
- 对于限流，针对的是不同用户、不同token进行限流，利用redisson基于redis中间件实现分布式限流，若redis宕机限流会降级，基于guava ratelimiter实现单机限流，以此保证限流服务可用性，以及避免限流服务失效，导致数据库被请求打垮。

# 分布式缓存问题解决方案
### 缓存更新/数据同步
- mysql如果执行了写操作(update/insert/delete)，使用canal订阅增量数据，通过kafka消息队列，异步更新redis中的数据，保证mysql与redis缓存的数据一致性
- caffeine本地缓存设置"定期刷新过期缓存": `expireAfterWrite()` 和 `refreshAfterWrite()`
- `expireAfterWrite()`：设置缓存淘汰时间
- `refreshAfterWrite()`：设置写入缓存后的刷新时间，刷新是基于访问被动触发的，支持异步和同步刷新。建议和`expireAfterWrite()`一起使用，能够保证即使缓存访问不到，也能在固定时间间隔后被淘汰，否则如果单独使用容易造成OOM
- 既然引入了缓存机制，就相当于舍弃了强一致性原则，而且引入缓存通常是为了减轻查询压力，可通过异步更新缓存的方式尽可能保证分布式缓存数据的一致性问题，保证最终一致性即可。根据具体业务来调整caffeine、redis、mysql三者的数据同步问题。

### 缓存穿透
当访问一个redis缓存和数据库都不存在的key时，此时请求会直接到数据库中查询，但是查不到任何数据，而且也没办法写缓存。这种情况缓存相当于没起作用，当流量过大时，数据库可能会挂掉。
- 接口校验：用户调用接口时，请求参数需要带上申请的token。接口根据token进行鉴权，过滤掉一些非法的接口请求
- 缓存空值：在配置文件中进行配置，允许caffeine和redis缓存null值，同时还需要给null值设置过期时间，防止一直占用null占用内存

### 缓存击穿
某一个热点 key，在缓存过期的一瞬间，同时有大量的请求打进来，由于此时缓存过期了，所以请求最终都会走到数据库，造成瞬时数据库请求量大、压力骤增，甚至可能打垮数据库。
- 加互斥锁：基于redisson实现分布式锁，在并发的多个请求中，只有第一个请求线程能拿到锁并执行数据库查询操作，其他的线程拿不到锁就阻塞等着，等到第一个线程将数据写入缓存后，直接走缓存
- caffeine默认使用异步机制加载缓存数据，可以有效防止缓存击穿。refreshAfterWrite策略在缓存过期后不会被回收，当再次访问时才会去刷新缓存，在新值没有加载完毕前，其他的线程访问始终返回旧值。同时设置`@Cacheable(sync=true)`，只有一个线程加载数据，其他线程均阻塞。

### 缓存雪崩
大量的热点 key 设置了相同的过期时间，导致缓存在同一时刻全部失效，造成瞬时数据库请求量大、压力骤增，引起雪崩，甚至导致数据库被打挂。缓存雪崩其实有点像“升级版的缓存击穿”，缓存击穿是一个热点 key，缓存雪崩是一组热点 key。
- 加互斥锁：同缓存击穿解决方案
- 热点key设置永不过期：既然缓存雪崩是因为过期导致的，我们可以不设置过期时间，可以根据具体业务设置refreshAfterWrite 异步刷新缓存
- 分散过期时间：可以给缓存的过期时间时加上一个随机值时间，使得每个 key 的过期时间分布开来，不会集中在同一时刻失效。可以自定义Expiry设置不同的过期时间计算规则，同时设置expireAfter()来达到分散过期时间的目的

### 限流
使用本地缓存减轻了redis和mysql集群的压力，但是仍要需要对请求进行流量控制和限流降级保护
- 基于redisson实现分布式限流器，针对不同用户不同token进行限流，QPS达到阈值后直接返回限流提醒
- 使用guava ratelimiter，当redis宕机时，分布式限流降级为单机限流，保证限流可用性和数据库稳定性
