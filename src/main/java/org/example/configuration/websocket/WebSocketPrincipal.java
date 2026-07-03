package org.example.configuration.websocket;

import java.security.Principal;

public record WebSocketPrincipal(Long userId) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
