package org.example.configuration.scheduler;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerLockManager {

    private static final String LOCK_PREFIX = "lock:";

    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public Optional<String> acquireLock(String key, Duration ttl) {

        String token = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + key, token, ttl);

        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    public void releaseLock(String key, String token) {

        redisTemplate.execute(RELEASE_SCRIPT, List.of(LOCK_PREFIX + key), token);
    }
}
