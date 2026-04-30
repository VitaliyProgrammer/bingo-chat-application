package org.example.configuration.listener;

import java.security.Principal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.service.RedisService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectEventListener {

    private final RedisService redisService;

    private final ApplicationMetricsService metricsService;

    @EventListener
    public void onConnect(SessionConnectEvent event) {

        extractUserId(event).ifPresent(userId -> {

            redisService.incrementSessions(userId);
            redisService.setUserOnline(userId);
            metricsService.incrementWebSocketConnect();

            log.info("WebSocket connected: userId={}, state={}",
                    userId, redisService.bumpPresenceState(userId));
        });
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {

        extractUserId(event).ifPresent(userId -> {

            redisService.decrementSessions(userId);

            if (redisService.getSessions(userId) > 0) {
                return;
            }

            redisService.setUserOffline(userId);
            metricsService.incrementWebSocketDisconnect();
            log.info("WebSocket disconnected: userId={}", userId);
        });
    }

    private Optional<Long> extractUserId(AbstractSubProtocolEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal principal = accessor.getUser();

        if (principal == null) {
            log.debug("WebSocket event without principal: type={}",
                    event.getClass().getSimpleName());
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(principal.getName()));
        } catch (NumberFormatException exception) {
            log.warn("Invalid principal name for WebSocket event: principal={}",
                    principal.getName());
            return Optional.empty();
        }
    }
}
