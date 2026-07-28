package org.example.controller.websocket;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.example.service.PresenceService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PresenceWebSocketController {

    private final PresenceService presenceService;

    @MessageMapping("/presence.ping")
    public void ping(Principal principal) {

        presenceService.heartbeat(principal);
    }

    @MessageMapping("/presence.lastSeen")
    public void updateLastSeen(Principal principal) {

        presenceService.updateLastSeen(principal);
    }
}
