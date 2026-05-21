package com.pratik.urlshortener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTestService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void testRedis() {
        redisTemplate.opsForValue().set("test", "hello");
        System.out.println(redisTemplate.opsForValue().get("test"));
    }
}