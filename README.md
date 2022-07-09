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
