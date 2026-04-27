package org.example.controller.websocket;

import lombok.RequiredArgsConstructor;
import org.example.service.PresenceService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PresenceWebSocketController {

    private final PresenceService presenceService;

    @MessageMapping("/presence.ping")
    public void ping() {

        presenceService.heartbeat();
    }

    @MessageMapping("/presence.lastSeen")
    public void updateLastSeen() {

        presenceService.updateLastSeen();
    }
}
