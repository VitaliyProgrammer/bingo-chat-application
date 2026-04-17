package org.example.service.redis;

public class RedisKeys {

    public static String userOnline(Long userId) {

        return "user:online: " + userId;
    }

    public static String userLastSeen(Long userId) {

        return "user:lastSeen: " + userId;
    }

    public static String unreadMessagesCount(Long userId, Long chaId) {

        return "chat: " + chaId + ":unread: " + userId;
    }
}
