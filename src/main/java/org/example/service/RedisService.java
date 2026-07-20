package org.example.service;

import java.time.Duration;

public interface RedisService {

    void setValue(String key, String value, Duration duration);

    String getValue(String key);

    void delete(String key);

    boolean existsKey(String key);

    void setUserOnline(Long userId);

    void setUserOnline(Long userId, Duration ttl);

    void setUserOffline(Long userId);

    boolean isUserOnline(Long userId);

    void incrementUnreadMessages(Long userId, Long chatId);

    void resetUnReadMessages(Long userId, Long chatId);

    int getUnreadMessages(Long userId, Long chatId);

    void setLastSeen(Long userId);

    Long getLastSeen(Long userId);

    boolean setIfAbsent(String key, String value, Duration ttl);

    long increment(String key);

    void expire(String key, Duration ttl);

    void incrementSessions(Long userId);

    void decrementSessions(Long userId);

    long getSessions(Long userId);

    long bumpPresenceState(Long userId);

    long getPresenceState(Long userId);
}
