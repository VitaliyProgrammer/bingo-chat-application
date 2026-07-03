package org.example.configuration.websocket.handler;

import lombok.RequiredArgsConstructor;
import org.example.configuration.websocket.WebSocketPrincipal;
import org.example.service.RedisService;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceHandler implements ApplicationListener<SessionConnectedEvent> {

    private final RedisService redisService;

    @Override
    public void onApplicationEvent(SessionConnectedEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() instanceof WebSocketPrincipal principal) {

            Long userId = principal.userId();

            redisService.setUserOnline(userId);
        }
    }
}
