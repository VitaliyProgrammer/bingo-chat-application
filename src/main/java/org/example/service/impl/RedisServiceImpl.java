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

    public void setValue(String key, String value) {

        redisTemplate.opsForValue().set(key, value);
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

        setUserOnline(userId, Duration.ofMinutes(5));
    }

    @Override
    public void setUserOnline(Long userId, Duration ttl) {

        redisTemplate.opsForValue().set(RedisKeys.userOnline(userId), "1", ttl);
    }

    @Override
    public void setUserOffline(Long userId) {

        redisTemplate.opsForValue().set(RedisKeys.userLastSeen(userId),
                System.currentTimeMillis());

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
}
