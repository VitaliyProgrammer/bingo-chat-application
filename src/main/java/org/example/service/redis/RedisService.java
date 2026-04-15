package org.example.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;


    public void setValue(String key, String value) {

        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {

        Object value = redisTemplate.opsForValue().get(key);

        return value != null ? value.toString() : null;
    }
}
