package com.example.cachej.service;

import com.alibaba.fastjson.JSON;
import com.example.cachej.config.CacheKey;
import com.example.cachej.domain.Student;
import com.example.cachej.mapper.StudentMapper;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@CacheConfig(cacheNames = "studentCache")
public class StudentServiceImpl implements StudentService {
    public static final Logger LOG = LoggerFactory.getLogger(StudentServiceImpl.class);

    @Autowired
    StudentMapper studentMapper;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;


    //先尝试从caffeine中获取student，开启sync是避免缓存击穿
    @Cacheable(key = "#id", value = CacheKey.CACHE_STUDENT_KEY, sync = true)
    @Override
    public Student getStudentInfo(Integer id) {
        RLock lock = redissonClient.getLock(CacheKey.CACHE_REDISSON_LOCK);

        lock.lock();
        LOG.info("[StudentService] lock, current thread: " + Thread.currentThread().getName());
        //如果一级缓存不存在，再尝试从redis二级缓存中获取student
        try {
            String jsonStr = (String) redisTemplate.opsForValue().get(CacheKey.CACHE_STUDENT_INFO + id);
            Student student = JSON.parseObject(jsonStr, Student.class); //反序列化
            if (student != null) {
                LOG.info("[StudentService] get student from redis, id=" + id);
                return student;
            }
        } catch (Exception e) {
            LOG.error("[StudentService] get student from redis exception: " + e.getMessage());
        }

        //如果redis也没有获取到，那么就从mysql中查询
        Student student = null;
        try {
            student = studentMapper.getStudentInfo(id);
            if (student != null) {
                LOG.info("[StudentService] get student from mysql");
                LOG.info("[StudentService] update redis");
                //更新redis缓存
                redisTemplate.opsForValue().set(CacheKey.CACHE_STUDENT_INFO + id, JSON.toJSONString(student), 10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOG.error("[StudentService] get student from mysql exception: " + e.getMessage());
        } finally {
            lock.unlock();
            LOG.info("[StudentService] unlock, current thread: " + Thread.currentThread().getName());
        }
        return student;
    }

    // 基于redis做分布式限流（redisson底层实现是redis+lua脚本，基于滑动窗口算法实现）
    public boolean flowControl(String token, String qps) {
        RRateLimiter limiter = redissonClient.getRateLimiter(CacheKey.CACHE_REDISSON_LIMITER + token);
        limiter.trySetRate(RateType.OVERALL, Long.parseLong(qps), 1, RateIntervalUnit.SECONDS);
        limiter.expire(10, TimeUnit.SECONDS);
        if (limiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            LOG.info("[StudentService] acquire 1 permit, not limit");
            return true;
        }
        LOG.info("[StudentService] not acquire permit, limit!");
        return false;
    }
}
