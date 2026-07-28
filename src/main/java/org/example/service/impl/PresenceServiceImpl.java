package org.example.service.impl;

import java.security.Principal;
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
    public void heartbeat(Principal principal) {

        Long userId = currentUserProvider.getAuthenticatedUser(principal).getId();

        redisService.setUserOnline(userId, ONLINE_TTL);
    }

    @Override
    public void updateLastSeen(Principal principal) {

        Long userId = currentUserProvider.getAuthenticatedUser(principal).getId();

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
