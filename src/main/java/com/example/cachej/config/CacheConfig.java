package com.example.cachej.config;


import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableConfigurationProperties(CacheProperties.class)
@Configuration
@EnableCaching //开启缓存功能
public class CacheConfig {

    /**
     * redis二级缓存配置
     *
     * @param redisConnectionFactory
     * @param cacheProperties
     * @return
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, CacheProperties cacheProperties) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        redisCacheConfiguration = redisCacheConfiguration.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        if (redisProperties.getTimeToLive() != null) {
            redisCacheConfiguration = redisCacheConfiguration.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            redisCacheConfiguration = redisCacheConfiguration.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            redisCacheConfiguration = redisCacheConfiguration.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            redisCacheConfiguration = redisCacheConfiguration.disableKeyPrefix();
        }

        return RedisCacheManager
                .builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                .cacheDefaults(redisCacheConfiguration).build();

    }

    /**
     * caffeine一级缓存配置
     *
     * @return
     */
    @Bean
    @Primary //使用了redis、caffeine两个cache，必须指定一个CacheManager为Primary，这里选择一级缓存caffeine
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<CaffeineCache> caches = initCaffeineCache();
        if (CollectionUtils.isEmpty(caches)) {
            return cacheManager;
        }
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    private List<CaffeineCache> initCaffeineCache() {
        List<CaffeineCache> caffeineCacheList = new ArrayList<>();
        CaffeineCache userCache = new CaffeineCache(CacheKey.CACHE_USER_KEY, Caffeine.newBuilder().recordStats()
                .expireAfterWrite(5, TimeUnit.SECONDS) //写入间隔5s淘汰
                .refreshAfterWrite(10, TimeUnit.SECONDS) // 写入后间隔10s刷新，该刷新是基于访问被动触发的，支持异步刷新和同步刷新，如果和 expireAfterWrite 组合使用，能够保证即使该缓存访问不到、也能在固定时间间隔后被淘汰，否则如果单独使用容易造成OOM
                .maximumSize(100) //缓存 key 的最大个数
                .weakKeys() //key设置为弱引用，在 GC 时可以直接淘汰；
                .weakValues() //value设置为弱引用，在 GC 时可以直接淘汰；
//                .softValues() //value设置为软引用，在内存溢出前可以直接淘汰
                .build());
        CaffeineCache studentCache = new CaffeineCache(CacheKey.CACHE_STUDENT_KEY, Caffeine.newBuilder().recordStats()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .maximumSize(100)
                .build());
        caffeineCacheList.add(userCache);
        caffeineCacheList.add(studentCache);
        return caffeineCacheList;
    }

    /**
     * redis配置
     *
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericFastJsonRedisSerializer());
        return redisTemplate;
    }
}
