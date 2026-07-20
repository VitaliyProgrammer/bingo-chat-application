package org.example.security.websocket;

import java.security.Principal;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.configuration.metrics.service.ApplicationMetricsService;
import org.example.repository.ChatRepository;
import org.example.security.audit.SecurityAuditService;
import org.example.security.websocket.exception.WebSocketAccessDeniedException;
import org.example.service.RedisService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSubscriptionGuard implements ChannelInterceptor {

    private static final String CHAT_PREFIX = "/topic/chat/";
    private final ChatRepository chatRepository;

    private final RedisService redisService;

    private final SecurityAuditService securityAuditService;

    private final ApplicationMetricsService metricsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String destination = accessor.getDestination();

            if (destination == null || !destination.startsWith(CHAT_PREFIX)) {
                return message;
            }

            Principal principal = accessor.getUser();

            if (principal == null) {
                securityAuditService.webSocketDenied(null, destination,
                        "Unauthenticated user!");

                log.warn("Denied subscribe: unauthenticated user!");
                throw new WebSocketAccessDeniedException("Unauthenticated user!");
            }

            Long userId = parseUserId(principal);
            Long chatId = parseChatId(destination);

            validateRateLimit(userId);

            boolean allowed = chatRepository.existsByIdAndParticipants_Id(chatId, userId);

            if (!allowed) {
                securityAuditService.webSocketDenied(userId, destination,
                        "No access to chat!");
                metricsService.incrementWebSocketDenied();

                log.warn("Denied subscribe: userId={}, chatId={}", userId, chatId);
                throw new WebSocketAccessDeniedException("No access to chat!");
            }
            log.info("Subscribe allowed: userId={}, chatId={}", userId, chatId);
        }
        return message;
    }

    private Long parseUserId(Principal principal) {
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException exception) {
            throw new WebSocketAccessDeniedException("Invalid principal!");
        }
    }

    private Long parseChatId(String destination) {

        String[] parts = destination.split("/");

        if (parts.length < 4) {
            throw new WebSocketAccessDeniedException("Invalid destination!");
        }

        try {
            return Long.parseLong(parts[3]);
        } catch (NumberFormatException exception) {
            throw new WebSocketAccessDeniedException("Invalid chatId!");
        }
    }

    private void validateRateLimit(Long userId) {

        String key = "websocket:subscribe:" + userId;

        // Allow up to 10 subscriptions per second to prevent connection drops on multiple subscriptions
        long count = redisService.increment(key);
        if (count == 1) {
            redisService.expire(key, Duration.ofSeconds(1));
        }

        if (count > 10) {
            throw new WebSocketAccessDeniedException("Too many subscribe requests!");
        }
    }
}
