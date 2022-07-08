package com.example.cachej.canal;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.cachej.config.CacheKey;
import com.example.cachej.domain.UserInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CanalConsumer {
    private static Logger LOG = LoggerFactory.getLogger(CanalConsumer.class);

    @Autowired
    RedisTemplate redisTemplate;

    //监听kafka消息队列中的binlog数据
    @KafkaListener(topics = "canalTopic")
    public void consumer(ConsumerRecord<?, ?> record) {
        String value = (String) record.value();
        LOG.info("[CanalConsumer] topic:{}, partition:{},key:{},value:{}", record.topic(),
                record.partition(), record.key(), record.value());
        String type = (String) JSONObject.parseObject(value).getString("type");
        String table = (String) JSONObject.parseObject(value).getString("table");
        boolean isDDL = (boolean) JSONObject.parseObject(value).getBoolean("isDdl");
        JSONArray data = JSONObject.parseObject(value).getJSONArray("data");

//        boolean isDDL = binLog.isDdl();
//        String type = binLog.getType();
        //判断是不是DDL语句，比如create、drop等一些操作，我们只关心mysql的增删改操作
        if (!isDDL) {
//            List<UserInfo> data = binLog.getData();
            //如果是新增操作，需要将数据同步到redis中，并设置过期时间100s
            if ("INSERT".equals(type) && "user_info".equals(table)) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    redisTemplate.opsForValue().set(CacheKey.CACHE_AUTH_USER_INFO + jsonObject.getInteger("id"), jsonObject.toJSONString(),
                            100, TimeUnit.SECONDS);
                }


            } else if ("UPDATE".equals(type)) {
                //如果是修改操作，需要将数据同步到redis中，并设置过期时间100s
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    redisTemplate.opsForValue().set(CacheKey.CACHE_AUTH_USER_INFO + jsonObject.getInteger("id"), jsonObject.toJSONString(),
                            100, TimeUnit.SECONDS);
                }
            } else if ("DELETE".equals(type)) {
                //如果是删除操作，需要把redis中的缓存数据也删除掉
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    redisTemplate.delete(CacheKey.CACHE_AUTH_USER_INFO + jsonObject.getInteger("id"));
                }
            }
        }
    }
}
