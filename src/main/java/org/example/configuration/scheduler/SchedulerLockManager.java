package org.example.configuration.scheduler;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerLockManager {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean acquireLock(String key, Duration ttl) {

        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent("lock:" + key, "1", ttl);

        return Boolean.TRUE.equals(result);
    }

    public void releaseLock(String key) {

        redisTemplate.delete("lock:" + key);
    }
}
