package org.example.service.impl;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.example.service.RedisService;
import org.example.service.redis.RedisKeys;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, String value, Duration ttl) {

        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public String getValue(String key) {

        Object value = redisTemplate.opsForValue().get(key);

        return value != null ? value.toString() : null;
    }

    @Override
    public void delete(String key) {

        redisTemplate.delete(key);
    }

    @Override
    public boolean existsKey(String key) {

        Boolean hasKey = redisTemplate.hasKey(key);

        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public void setUserOnline(Long userId) {

        setUserOnline(userId, Duration.ofSeconds(40));
    }

    @Override
    public void setUserOnline(Long userId, Duration ttl) {

        redisTemplate.opsForValue().set(RedisKeys.userOnline(userId), "1", ttl);
    }

    @Override
    public void setUserOffline(Long userId) {

        setLastSeen(userId);

        redisTemplate.delete(RedisKeys.userOnline(userId));
    }

    @Override
    public boolean isUserOnline(Long userId) {

        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.userOnline(userId)));
    }

    @Override
    public void incrementUnreadMessages(Long userId, Long chatId) {

        redisTemplate.opsForValue().increment(RedisKeys.unreadMessagesCount(userId, chatId));
    }

    @Override
    public int getUnreadMessages(Long userId, Long chatId) {

        Object value = redisTemplate.opsForValue()
                .get(RedisKeys.unreadMessagesCount(userId, chatId));

        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    @Override
    public void resetUnReadMessages(Long userId, Long chatId) {

        redisTemplate.delete(RedisKeys.unreadMessagesCount(userId, chatId));
    }

    @Override
    public void setLastSeen(Long userId) {

        redisTemplate.opsForValue().set(RedisKeys.userLastSeen(userId),
                String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public Long getLastSeen(Long userId) {

        Object value = redisTemplate.opsForValue()
                .get(RedisKeys.userLastSeen(userId));

        return value == null ? null : Long.parseLong(value.toString());
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {

        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public long increment(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        return value == null ? 0L : value;
    }

    @Override
    public void expire(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, Duration window) {

        long count = increment(key);
        if (count == 1) {
            expire(key, window);
        }

        return count <= maxRequests;
    }

    @Override
    public void incrementSessions(Long userId) {

        redisTemplate.opsForValue().increment(RedisKeys.userSessions(userId));
    }

    @Override
    public void decrementSessions(Long userId) {

        Long value = redisTemplate.opsForValue().decrement(RedisKeys.userSessions(userId));

        if (value != null && value <= 0) {
            redisTemplate.delete(RedisKeys.userSessions(userId));
        }
    }

    @Override
    public long getSessions(Long userId) {

        return getLongValue(RedisKeys.userSessions(userId));
    }

    @Override
    public long getPresenceState(Long userId) {

        return getLongValue(RedisKeys.userPresenceState(userId));
    }

    @Override
    public long bumpPresenceState(Long userId) {

        String key = RedisKeys.userPresenceState(userId);
        Long version = redisTemplate.opsForValue().increment(key);

        return version == null ? 0L : version;
    }

    private long getLongValue(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
