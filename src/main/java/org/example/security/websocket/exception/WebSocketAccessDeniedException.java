package org.example.security.websocket.exception;

public class WebSocketAccessDeniedException extends RuntimeException {
    public WebSocketAccessDeniedException(String message) {
        super(message);
    }
}
