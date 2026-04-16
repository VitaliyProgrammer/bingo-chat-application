package org.example.configuration.websocket.handler;

import lombok.RequiredArgsConstructor;
import org.example.service.RedisService;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketDisconnectHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final RedisService redisService;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() != null) {

            String userId = accessor.getUser().getName();

            redisService.setUserOffline(Long.valueOf(userId));
        }
    }
}
