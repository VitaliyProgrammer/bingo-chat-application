package org.example.configuration.metrics.model;

public final class MetricsNames {

    public static final String WEBSOCKET_CONNECT = "websocket.connect";
    public static final String WEBSOCKET_DISCONNECT = "websocket.disconnect";
    public static final String LOGIN_FAILED = "authentication.login.failed";
    public static final String JWT_TOKEN_INVALID = "authentication.jwt.invalid";
    public static final String WEBSOCKET_DENIED = "websocket.access.denied";
    public static final String MESSAGE_SENT = "message.sent";
    public static final String OUTBOX_PROCESSED = "outbox.processed";
    public static final String OUTBOX_FAILED = "outbox.failed";
    public static final String OUTBOX_LATENCY = "outbox.processing.time";
    public static final String WEB_SOCKET_ERROR = "websocket.error";

    public MetricsNames() {
    }
}
