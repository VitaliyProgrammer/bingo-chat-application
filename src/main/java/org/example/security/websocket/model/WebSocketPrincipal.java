package org.example.security.websocket.model;

import java.security.Principal;

public record WebSocketPrincipal(Long userId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
