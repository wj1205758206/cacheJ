package com.example.cachej.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.cachej.config.CacheKey;
import com.example.cachej.domain.UserInfo;
import com.example.cachej.mapper.UserMapper;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@CacheConfig(cacheNames = "userCache")
public class UserServiceImpl implements UserService {
    public static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    UserMapper userMapper;
    @Autowired
    RedisTemplate redisTemplate;

    //先尝试从caffeine中获取user，开启sync是避免缓存击穿
    @Cacheable(key = "#id", value = CacheKey.CACHE_USER_KEY, sync = true)
    @Override
    public UserInfo getUser(Integer id) {
        //如果一级缓存不存在，再尝试从redis二级缓存中获取user
        try {
            String jsonStr = (String) redisTemplate.opsForValue().get(CacheKey.CACHE_AUTH_USER_INFO + id);
            UserInfo user = JSON.parseObject(jsonStr, UserInfo.class); //反序列化
            if (user != null) {
                LOG.info("[UserService] get user from redis, id=" + id);
                return user;
            }
        } catch (Exception e) {
            LOG.error("[UserService] get user from redis exception: " + e.getMessage());
        }

        //如果redis也没有获取到，那么就从mysql中查询
        UserInfo user = null;
        try {
            user = userMapper.getUser(id);
            if (user != null) {
                LOG.info("[UserService] get user from mysql");
                LOG.info("[UserService] update redis");
                //更新redis缓存
                redisTemplate.opsForValue().set(CacheKey.CACHE_AUTH_USER_INFO + id, JSON.toJSONString(user), 10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOG.error("[UserService] get user from mysql exception: " + e.getMessage());
        }
        return user;
    }

    @Caching(
            evict = {@CacheEvict(key = "'getAllUsers'")},
            put = {@CachePut(key = "#user.id", value = CacheKey.CACHE_USER_KEY)} //caffeine一级缓存
    )
    @Override
    public UserInfo addUser(UserInfo user) {
//        //双写
//        try {
//            //1.写redis二级缓存
//            redisTemplate.opsForValue().set(
//                    CacheKey.CACHE_USER_INFO + user.getId(), JSON.toJSONString(user), 10, TimeUnit.SECONDS);
//        } catch (Exception e) {
//            LOG.error("[UserService] write redis cache exception: " + e.getMessage());
//        }

        try {
            //2.持久化写到mysql
            userMapper.addUser(user);
        } catch (Exception e) {
            LOG.error("[UserService] write mysql exception: " + e.getMessage());
        }
        LOG.info("add user: " + user.toString());
        return user;
    }


    @Caching(evict = {
            @CacheEvict(key = "'getAllUsers'"),
            @CacheEvict(key = "#id", value = CacheKey.CACHE_USER_KEY)} //删除caffeine缓存
    )
    @Override
    public void deleteUser(Integer id) {
        try {
            //删除mysql
            userMapper.deleteUser(id);
            LOG.info("[UserService] delete user from mysql success");
//            //删除redis
//            redisTemplate.delete(CacheKey.CACHE_USER_INFO + id);
//            LOG.info("[UserService] delete user from redis success");
        } catch (Exception e) {
            LOG.error("[UserService] delete user exception: " + e.getMessage());
        }
    }


    @Caching(evict = {
            @CacheEvict(key = "'getAllUsers'")
    }, put = {@CachePut(key = "#user.id", value = CacheKey.CACHE_USER_KEY)}) //update方法执行成功之后，也更新caffeine缓存
    @Override
    public UserInfo updateUser(UserInfo user) {
        UserInfo oldUser = null;
        try {
            oldUser = userMapper.getUser(user.getId());
            if (oldUser == null) {
                LOG.error("[UserService] update user fail, id not exist");
                return null;
            }
            oldUser.setUsername(user.getUsername());
            oldUser.setProduct(user.getProduct());
            oldUser.setDepartment(user.getDepartment());
            oldUser.setToken(user.getToken());
            oldUser.setQps(user.getQps());
            //更新mysql
            userMapper.updateUser(user);
//            //更新redis
//            redisTemplate.opsForValue().set(CacheKey.CACHE_USER_INFO + oldUser.getId(), JSON.toJSON(oldUser), 10, TimeUnit.SECONDS);
            return oldUser;
        } catch (Exception e) {
            LOG.error("[UserService] update user exception: " + e.getMessage());
        }
        return oldUser;
    }

    @Cacheable(key = "'getAllUsers'", value = CacheKey.CACHE_USER_KEY) //先从caffeine缓存获取all users
    @Override
    public List<UserInfo> getAllUsers() {
        List<UserInfo> allUsers = null;
        //再尝试从redis中获取
        try {
            allUsers = redisTemplate.opsForList().range(CacheKey.CACHE_AUTH_USER_INFO + "allUsers", 0, -1);
            if (CollectionUtils.isNotEmpty(allUsers)) {
                return allUsers;
            }
            //从mysql查询
            allUsers = userMapper.getAllUsers();
            LOG.info("[UserService] get all users from mysql");
            //更新redis缓存
            redisTemplate.opsForList().rightPushAll(CacheKey.CACHE_AUTH_USER_INFO + "allUsers", allUsers);
            LOG.info("[UserService] update all users to redis");
        } catch (Exception e) {
            LOG.error("[UserService] get all users exception: " + e.getMessage());
        }
        return allUsers;
    }

    //先尝试从caffeine中获取user，开启sync是避免缓存击穿
    @Cacheable(key = "#token", value = CacheKey.CACHE_USER_KEY, sync = true)
    @Override
    public UserInfo getUserByToken(String token) {
        //如果一级缓存不存在，再尝试从redis二级缓存中获取user
        try {
            String jsonStr = (String) redisTemplate.opsForValue().get(CacheKey.CACHE_AUTH_USER_INFO + token);
            UserInfo user = JSON.parseObject(jsonStr, UserInfo.class); //反序列化
            if (user != null) {
                LOG.info("[UserService] get user from redis, token=" + token);
                return user;
            }
        } catch (Exception e) {
            LOG.error("[UserService] get user from redis exception: " + e.getMessage());
        }

        //如果redis也没有获取到，那么就从mysql中查询
        UserInfo user = null;
        try {
            user = userMapper.getUserByToken(token);
            if (user != null) {
                LOG.info("[UserService] get user from mysql");
                LOG.info("[UserService] update redis");
                //更新redis缓存
                redisTemplate.opsForValue().set(CacheKey.CACHE_AUTH_USER_INFO + token, JSON.toJSONString(user), 10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOG.error("[UserService] get user from mysql exception: " + e.getMessage());
        }
        return user;
    }
}
