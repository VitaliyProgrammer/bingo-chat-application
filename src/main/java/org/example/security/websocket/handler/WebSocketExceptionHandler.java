package org.example.security.websocket.handler;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.exception.InvalidJwtTokenException;
import org.example.exception.JwtTokenExpiredException;
import org.example.exception.UserNotFoundException;
import org.example.security.websocket.exception.WebSocketAccessDeniedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketExceptionHandler extends StompSubProtocolErrorHandler {

    private static final String INTERNAL_ERROR = "WEBSOCKET_INTERNAL_ERROR";
    private static final String WEBSOCKET_FORBIDDEN = "WEBSOCKET_FORBIDDEN";
    private static final String WEBSOCKET_JWT_TOKEN_INVALID = "WEBSOCKET_JWT_TOKEN_INVALID";
    private static final String WEBSOCKET_JWT_TOKEN_EXPIRED = "WEBSOCKET_JWT_TOKEN_EXPIRED";
    private static final String WEBSOCKET_USER_NOT_FOUND = "WEBSOCKET_USER_NOT_FOUND";
    private final ApplicationMetricsService metricsService;

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
            Message<byte[]> clientMessage, Throwable exception) {

        Throwable root = resolveRootException(exception);

        return tryHandleAccessDenied(root, clientMessage)
                .or(() -> tryHandleInvalidJwtToken(root, clientMessage))
                .or(() -> tryHandleExpiredJwtToken(root, clientMessage))
                .or(() -> tryHandleUserNotFound(root, clientMessage))
                .orElseGet(() -> {
                    metricsService.incrementWebSocketError();

                    log.error("Unhandled WebSocket error", exception);
                    return buildError(INTERNAL_ERROR, "Internal WebSocket error!",
                            clientMessage);
                });
    }

    private Optional<Message<byte[]>> tryHandleAccessDenied(Throwable root,
                                                            Message<byte[]> clientMessage) {

        if (root instanceof WebSocketAccessDeniedException exception) {
            return Optional.of(buildError(WEBSOCKET_FORBIDDEN, exception.getMessage(),
                    clientMessage));
        }
        return Optional.empty();
    }

    private Optional<Message<byte[]>> tryHandleInvalidJwtToken(Throwable root,
                                                               Message<byte[]> clientMessage) {

        if (root instanceof InvalidJwtTokenException exception) {
            return Optional.of(buildError(WEBSOCKET_JWT_TOKEN_INVALID, exception.getMessage(),
                    clientMessage));
        }
        return Optional.empty();
    }

    private Optional<Message<byte[]>> tryHandleExpiredJwtToken(Throwable root,
                                                               Message<byte[]> clientMessage) {

        if (root instanceof JwtTokenExpiredException exception) {
            return Optional.of(buildError(WEBSOCKET_JWT_TOKEN_EXPIRED, exception.getMessage(),
                    clientMessage));
        }
        return Optional.empty();
    }

    private Optional<Message<byte[]>> tryHandleUserNotFound(Throwable root,
                                                            Message<byte[]> clientMessage) {

        if (root instanceof UserNotFoundException exception) {
            return Optional.of(buildError(WEBSOCKET_USER_NOT_FOUND, exception.getMessage(),
                    clientMessage));
        }
        return Optional.empty();
    }

    private Throwable resolveRootException(Throwable exception) {

        Throwable root = exception != null ? exception
                : new RuntimeException("Unknown WebSocket error!");

        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private Message<byte[]> buildError(String code, String message,
                                       Message<byte[]> clientMessage) {

        String payload = "{\"error\":\"" + message + "\",\"code\":\"" + code + "\"}";

        MessageHeaders headers = clientMessage != null ? clientMessage.getHeaders()
                : new MessageHeaders(Map.of());

        log.warn("WebSocket error sent: code={}, message={}", code, message);

        return MessageBuilder.createMessage(payload.getBytes(StandardCharsets.UTF_8), headers);
    }
}
