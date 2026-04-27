package org.example.service.impl;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.example.security.CurrentUserProvider;
import org.example.service.PresenceService;
import org.example.service.RedisService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(40);

    private final CurrentUserProvider currentUserProvider;

    private final RedisService redisService;

    @Override
    public void heartbeat() {

        Long userId = currentUserProvider.getAuthenticatedUser().getId();

        redisService.setUserOnline(userId, ONLINE_TTL);
    }

    @Override
    public void updateLastSeen() {

        Long userId = currentUserProvider.getAuthenticatedUser().getId();

        redisService.setLastSeen(userId);
    }

    @Override
    public void disconnect() {

        Long userId = currentUserProvider.getAuthenticatedUser().getId();

        if (redisService.getSessions(userId) <= 0) {
            redisService.setUserOffline(userId);
        }
    }
}
